package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.RazorTuneApplication
import com.example.data.PerformanceSettings
import com.example.data.PlayEvent
import com.example.model.PlaybackUiState
import com.example.model.SleepTimerState
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
class PlaybackController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _audioSessionId = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var preloadJob: Job? = null
    private var activeQueue: List<Track> = emptyList()
    private var performanceSettings: PerformanceSettings = PerformanceSettings()
    private var lastPreloadedKey: String? = null

    private var trackedTrack: Track? = null
    private var accumulatedListenMs: Long = 0L
    private var lastPlayTickElapsedMs: Long? = null

    val audioEffectsManager = RazorTuneAudioEffectsManager(RazorTuneApplication.instance.audioEffectsStore)
    val audioVisualizerManager = AudioVisualizerManager()

    private val pendingActions = mutableListOf<(MediaController) -> Unit>()

    init {
        connectIfNeeded()
        observePerformanceSettings()
    }

    fun attachAudioEffectsSession(audioSessionId: Int) {
        _audioSessionId.value = audioSessionId
        audioEffectsManager.attachToAudioSession(audioSessionId)
        audioVisualizerManager.attachToAudioSession(audioSessionId)
    }

    fun releaseAudioEffectsSession() {
        _audioSessionId.value = C.AUDIO_SESSION_ID_UNSET
        audioEffectsManager.detachSession()
        audioVisualizerManager.detachSession()
    }

    fun release() {
        audioEffectsManager.release()
        audioVisualizerManager.release()
    }

    fun isTrackLiked(videoId: String?): Boolean =
        !videoId.isNullOrBlank() && videoId in _likedVideoIds.value

    fun updateTrackLikedState(videoId: String, liked: Boolean) {
        if (videoId.isBlank()) return
        _likedVideoIds.update { set ->
            if (liked) set + videoId else set - videoId
        }
    }

    private fun observePerformanceSettings() {
        scope.launch {
            RazorTuneApplication.instance.performanceStore.settingsFlow.collect { settings ->
                performanceSettings = settings
                if (!settings.preloadNextTrack) {
                    preloadJob?.cancel()
                    preloadJob = null
                    lastPreloadedKey = null
                } else {
                    schedulePreloadNextTrack()
                }
            }
        }
    }

    private fun connectIfNeeded() {
        if (controllerFuture != null) return
        val token = SessionToken(
            context,
            ComponentName(context, RazorTunePlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller ->
                        mediaController = controller
                        attachListener(controller)
                        syncFromController(controller)
                        val queued = pendingActions.toList()
                        pendingActions.clear()
                        queued.forEach { action -> action(controller) }
                    }
                    .onFailure { err ->
                        _playbackState.update {
                            it.copy(errorMessage = err.localizedMessage ?: "Failed to connect player")
                        }
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        val current = mediaController
        if (current != null) {
            action(current)
        } else {
            pendingActions += action
            connectIfNeeded()
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        val effectiveQueue = if (queue.any { it.videoId == track.videoId }) queue else listOf(track)
        val startIndex = effectiveQueue.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
        playQueue(effectiveQueue, startIndex)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        flushListeningSession(resetBelowThreshold = true)
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        activeQueue = tracks

        val mediaItems = tracks.map { track -> track.toMediaItem() }
        val selectedTrack = tracks[safeIndex]
        trackedTrack = selectedTrack
        accumulatedListenMs = 0L
        lastPlayTickElapsedMs = null

        _playbackState.update {
            it.copy(
                currentTrack = selectedTrack,
                queue = tracks,
                currentIndex = safeIndex,
                isBuffering = true,
                errorMessage = null,
                durationMs = (selectedTrack.durationSeconds ?: 0) * 1000L,
                positionMs = 0L,
                hasNext = safeIndex < tracks.lastIndex,
                hasPrevious = safeIndex > 0,
            )
        }

        withController { controller ->
            controller.setMediaItems(mediaItems, safeIndex, 0L)
            controller.prepare()
            controller.play()
        }
        schedulePreloadNextTrack(safeIndex)
    }

    fun playTracksFromAuto(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val selectedTrack = tracks[safeIndex]

        if (trackedTrack?.videoId != selectedTrack.videoId || activeQueue.size != tracks.size) {
            flushListeningSession(resetBelowThreshold = true)
            trackedTrack = selectedTrack
            accumulatedListenMs = 0L
        }
        activeQueue = tracks

        val controller = mediaController
        _playbackState.update {
            it.copy(
                currentTrack = selectedTrack,
                queue = tracks,
                currentIndex = safeIndex,
                isBuffering = controller?.playbackState == Player.STATE_BUFFERING,
                errorMessage = null,
                durationMs = controller?.duration?.takeIf { d -> d > 0L }
                    ?: ((selectedTrack.durationSeconds ?: 0) * 1000L),
                positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                hasNext = safeIndex < tracks.lastIndex,
                hasPrevious = safeIndex > 0,
            )
        }
        if (controller != null && controller.isPlaying) {
            lastPlayTickElapsedMs = SystemClock.elapsedRealtime()
            startTicker(controller)
        }
        schedulePreloadNextTrack(safeIndex)
    }

    fun toggleRepeatMode() {
        withController { controller ->
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun toggleShuffleMode() {
        withController { controller ->
            val newShuffle = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newShuffle
            _shuffleModeEnabled.value = newShuffle
        }
    }

    fun flushPendingListenTime() {
        flushListeningSession(resetBelowThreshold = false)
    }

    fun togglePlayPause() {
        withController { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE && controller.mediaItemCount > 0) {
                    controller.prepare()
                }
                controller.play()
            }
        }
    }

    fun skipToNext() {
        withController { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                controller.play()
            }
        }
    }

    fun skipToPrevious() {
        withController { controller ->
            if (controller.currentPosition > 3_500L || !controller.hasPreviousMediaItem()) {
                controller.seekTo(0L)
            } else {
                controller.seekToPreviousMediaItem()
                controller.play()
            }
        }
    }

    fun skipToQueueIndex(index: Int) {
        withController { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.seekTo(index, 0L)
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        withController { controller ->
            val clamped = positionMs.coerceAtLeast(0L)
            controller.seekTo(clamped)
            _playbackState.update { it.copy(positionMs = clamped) }
        }
    }

    fun addToQueue(track: Track) {
        if (activeQueue.isEmpty() || _playbackState.value.currentTrack == null) {
            playTrack(track)
            return
        }
        val updatedQueue = activeQueue + track
        activeQueue = updatedQueue
        val mediaItem = track.toMediaItem()
        withController { controller ->
            controller.addMediaItem(mediaItem)
            syncFromController(controller)
        }
        schedulePreloadNextTrack()
    }

    fun startSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerState.value = SleepTimerState.Off
            return
        }
        val totalSeconds = minutes * 60L
        _sleepTimerState.value = SleepTimerState.Countdown(totalSeconds)
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0L && isActive) {
                delay(1.seconds)
                remaining--
                _sleepTimerState.value = SleepTimerState.Countdown(remaining)
            }
            if (isActive) {
                withController { it.pause() }
                _sleepTimerState.value = SleepTimerState.Off
            }
        }
    }

    fun startSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerState.value = SleepTimerState.EndOfTrack
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerState.value = SleepTimerState.Off
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    private fun attachListener(controller: MediaController) {
        controller.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    syncFromController(controller)
                    if (isPlaying) {
                        val activeTrack = activeQueue.getOrNull(controller.currentMediaItemIndex)
                            ?: controller.currentMediaItem?.toTrackFallback()
                        if (trackedTrack == null) {
                            trackedTrack = activeTrack
                        } else if (activeTrack != null && trackedTrack?.videoId != activeTrack.videoId) {
                            flushListeningSession(resetBelowThreshold = true)
                            trackedTrack = activeTrack
                        }
                        lastPlayTickElapsedMs = SystemClock.elapsedRealtime()
                        startTicker(controller)
                    } else {
                        stopTicker()
                        accumulateActiveTime()
                        lastPlayTickElapsedMs = null
                        if (!controller.playWhenReady ||
                            controller.playbackState == Player.STATE_ENDED ||
                            controller.playbackState == Player.STATE_IDLE
                        ) {
                            flushListeningSession(
                                resetBelowThreshold = controller.playbackState == Player.STATE_ENDED,
                            )
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (_sleepTimerState.value is SleepTimerState.EndOfTrack) {
                            _sleepTimerState.value = SleepTimerState.Off
                        }
                        flushListeningSession(resetBelowThreshold = true)
                    }
                    syncFromController(controller)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (_sleepTimerState.value is SleepTimerState.EndOfTrack &&
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                    ) {
                        controller.pause()
                        _sleepTimerState.value = SleepTimerState.Off
                    }
                    val nextTrack = activeQueue.getOrNull(controller.currentMediaItemIndex)
                        ?: mediaItem?.toTrackFallback()
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                        (trackedTrack != null && trackedTrack?.videoId != nextTrack?.videoId)
                    ) {
                        flushListeningSession(resetBelowThreshold = true)
                    }
                    trackedTrack = nextTrack
                    lastPlayTickElapsedMs = if (controller.isPlaying) SystemClock.elapsedRealtime() else null
                    syncFromController(controller)
                    schedulePreloadNextTrack(controller.currentMediaItemIndex)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                    syncFromController(controller)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                    syncFromController(controller)
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    attachAudioEffectsSession(audioSessionId)
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = error.localizedMessage ?: "Playback failed for this track",
                        )
                    }
                }
            },
        )
    }

    private fun syncFromController(controller: MediaController) {
        if (activeQueue.isEmpty() && controller.mediaItemCount > 0) {
            val restored = mutableListOf<Track>()
            for (i in 0 until controller.mediaItemCount) {
                restored += controller.getMediaItemAt(i).toTrackFallback()
            }
            activeQueue = restored
        }

        val index = controller.currentMediaItemIndex
        val currentTrack = activeQueue.getOrNull(index)
            ?: controller.currentMediaItem?.toTrackFallback()
        val reportedDuration = controller.duration.takeIf { it > 0L }
            ?: ((currentTrack?.durationSeconds ?: 0) * 1000L)

        _repeatMode.value = controller.repeatMode
        _shuffleModeEnabled.value = controller.shuffleModeEnabled

        _playbackState.update { prev ->
            prev.copy(
                currentTrack = currentTrack,
                queue = activeQueue,
                currentIndex = index,
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = reportedDuration.coerceAtLeast(0L),
                hasNext = controller.hasNextMediaItem(),
                hasPrevious = controller.hasPreviousMediaItem() || controller.currentPosition > 3_500L,
            )
        }
    }

    private fun startTicker(controller: MediaController) {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive && controller.isPlaying) {
                accumulateActiveTime()
                val reportedDuration = controller.duration.takeIf { it > 0L }
                    ?: _playbackState.value.durationMs
                _playbackState.update {
                    it.copy(
                        positionMs = controller.currentPosition.coerceAtLeast(0L),
                        durationMs = reportedDuration.coerceAtLeast(0L),
                    )
                }
                delay(400.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun accumulateActiveTime() {
        val lastTick = lastPlayTickElapsedMs ?: return
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastTick).coerceAtLeast(0L)
        accumulatedListenMs += delta
        lastPlayTickElapsedMs = now
    }

    private fun flushListeningSession(resetBelowThreshold: Boolean) {
        accumulateActiveTime()
        lastPlayTickElapsedMs = null
        val track = trackedTrack ?: run {
            if (resetBelowThreshold) accumulatedListenMs = 0L
            return
        }
        val listenedMs = accumulatedListenMs
        if (listenedMs >= MIN_TRACKED_LISTEN_MS && track.videoId.isNotBlank()) {
            accumulatedListenMs = 0L
            val event = PlayEvent(
                videoId = track.videoId,
                title = track.title,
                artist = track.artist,
                album = track.album,
                thumbnailUrl = track.thumbnailUrl,
                durationListenedMs = listenedMs,
                timestampMs = System.currentTimeMillis(),
            )
            scope.launch(Dispatchers.IO) {
                runCatching { RazorTuneApplication.instance.statsStore.recordPlayEvent(event) }
            }
        } else if (resetBelowThreshold) {
            accumulatedListenMs = 0L
        }
    }

    private fun schedulePreloadNextTrack(
        currentIndexOverride: Int = _playbackState.value.currentIndex,
    ) {
        val settings = performanceSettings
        if (!settings.preloadNextTrack) return
        val nextTrack = activeQueue.getOrNull(currentIndexOverride + 1) ?: return
        val videoId = nextTrack.videoId
        if (videoId.isBlank()) return

        val app = RazorTuneApplication.instance
        if (app.downloadManager.getDownloadedFile(videoId) != null) return

        val key = "$videoId:${settings.audioQuality.name}"
        if (lastPreloadedKey == key) return
        lastPreloadedKey = key

        preloadJob?.cancel()
        preloadJob = scope.launch(Dispatchers.IO) {
            app.repository.prewarmAudioStream(videoId, settings.audioQuality)
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            durationSeconds?.let { putInt(EXTRA_DURATION_SEC, it) }
            album?.let { putString(EXTRA_ALBUM, it) }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(thumbnailUrl?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(RazorTunePlaybackService.buildMediaUri(videoId))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toTrackFallback(): Track {
        val meta = mediaMetadata
        return Track(
            videoId = mediaId,
            title = meta.title?.toString().orEmpty().ifBlank { "Untitled Track" },
            artist = meta.artist?.toString().orEmpty().ifBlank { "YouTube Music" },
            album = meta.albumTitle?.toString(),
            durationSeconds = meta.extras?.getInt(EXTRA_DURATION_SEC)?.takeIf { it > 0 },
            thumbnailUrl = meta.artworkUri?.toString(),
        )
    }

    companion object {
        private const val EXTRA_DURATION_SEC = "duration_sec"
        private const val EXTRA_ALBUM = "album"
        private const val MIN_TRACKED_LISTEN_MS = 15_000L
    }
}