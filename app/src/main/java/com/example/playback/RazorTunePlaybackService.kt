package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.RazorTuneApplication
import com.example.data.AudioQualityMode
import com.example.model.HomeCardItem
import com.example.model.PlaylistSummary
import com.example.model.SearchFilterOption
import com.example.model.SearchResultItem
import com.example.model.Track
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class RazorTunePlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val playlistCache = ConcurrentHashMap<String, PlaylistSummary>()
    private val playlistTracksCache = ConcurrentHashMap<String, List<Track>>()
    private val trackCache = ConcurrentHashMap<String, Track>()
    private val searchResultsCache = ConcurrentHashMap<String, List<Track>>()

    @Volatile
    private var homeTracksCache: List<Track> = emptyList()

    @Volatile
    private var downloadedTracksCache: List<Track> = emptyList()

    @Volatile
    private var currentAudioQuality: AudioQualityMode = AudioQualityMode.HIGH

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val repository = RazorTuneApplication.instance.repository
        val downloadManager = RazorTuneApplication.instance.downloadManager

        val defaultHeaders = mapOf(
            "User-Agent" to DEFAULT_YT_MUSIC_USER_AGENT,
            "Origin" to YT_MUSIC_ORIGIN,
            "Referer" to YT_MUSIC_REFERER,
        )

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(DEFAULT_YT_MUSIC_USER_AGENT)
            .setDefaultRequestProperties(defaultHeaders)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val upstreamDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val resolvingDataSourceFactory: DataSource.Factory = ResolvingDataSource.Factory(
            upstreamDataSourceFactory,
        ) { dataSpec ->
            if (dataSpec.uri.scheme == SCHEME_RAZORTUNE) {
                val videoId = dataSpec.uri.host
                    ?: dataSpec.uri.lastPathSegment
                    ?: throw IllegalArgumentException("Missing videoId in media URI: ${dataSpec.uri}")

                val localFile = downloadManager.getDownloadedFile(videoId)
                if (localFile != null && localFile.exists() && localFile.length() > 0L) {
                    dataSpec.buildUpon()
                        .setUri(Uri.fromFile(localFile))
                        .build()
                } else {
                    val quality = currentAudioQuality
                    val stream = runBlocking(Dispatchers.IO) {
                        repository.resolveAudioStream(videoId, quality)
                    }

                    val mergedHeaders = LinkedHashMap<String, String>().apply {
                        put("Origin", YT_MUSIC_ORIGIN)
                        put("Referer", YT_MUSIC_REFERER)
                        put("User-Agent", DEFAULT_YT_MUSIC_USER_AGENT)
                        stream.headers.forEach { (k, v) ->
                            if (v.isNotBlank()) put(k, v)
                        }
                    }

                    mergedHeaders["User-Agent"]?.let { resolvedUa ->
                        httpDataSourceFactory.setUserAgent(resolvedUa)
                    }

                    dataSpec.buildUpon()
                        .setUri(Uri.parse(stream.audioUrl))
                        .setHttpRequestHeaders(mergedHeaders)
                        .build()
                }
            } else {
                dataSpec
            }
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exoPlayer = player

        val playbackController = RazorTuneApplication.instance.playbackController
        playbackController.attachAudioEffectsSession(player.audioSessionId)
        player.addListener(
            object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    playbackController.attachAudioEffectsSession(audioSessionId)
                }
            },
        )

        serviceScope.launch(Dispatchers.Main.immediate) {
            RazorTuneApplication.instance.performanceStore.settingsFlow.collect { settings ->
                currentAudioQuality = settings.audioQuality
                exoPlayer?.skipSilenceEnabled = settings.skipSilence
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val initialLiked = playbackController.isTrackLiked(
            playbackController.playbackState.value.currentTrack?.videoId,
        )
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, RazorTuneLibraryCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .setCustomLayout(listOf(buildLikeCommandButton(initialLiked)))
            .build()

        serviceScope.launch(Dispatchers.Main.immediate) {
            combine(
                playbackController.playbackState.map { it.currentTrack?.videoId },
                playbackController.likedVideoIds,
            ) { trackId, likedIds ->
                trackId != null && trackId in likedIds
            }
                .distinctUntilChanged()
                .collect { isLiked ->
                    updateLikeCustomLayout(isLiked)
                }
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildLikeCommandButton(isLiked: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isLiked) "Liked" else "Like")
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .setIconResId(if (isLiked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
            .setEnabled(true)
            .build()
    }

    fun updateLikeCustomLayout(isLiked: Boolean) {
        mediaLibrarySession?.setCustomLayout(listOf(buildLikeCommandButton(isLiked)))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            RazorTuneApplication.instance.playbackController.flushPendingListenTime()
        }
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching {
            val controller = RazorTuneApplication.instance.playbackController
            controller.flushPendingListenTime()
            controller.releaseAudioEffectsSession()
        }
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        exoPlayer = null
        mediaLibrarySession = null
        super.onDestroy()
    }

    private inner class RazorTuneLibraryCallback : MediaLibrarySession.Callback {

        private val app get() = RazorTuneApplication.instance
        private val repository get() = app.repository
        private val downloadManager get() = app.downloadManager

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val availableSessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                    .build()
            val activeTrackId = app.playbackController.playbackState.value.currentTrack?.videoId
                ?: session.player.currentMediaItem?.mediaId
            val isLiked = app.playbackController.isTrackLiked(activeTrackId)
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setCustomLayout(listOf(buildLikeCommandButton(isLiked)))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != ACTION_TOGGLE_LIKE) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            val currentTrack = app.playbackController.playbackState.value.currentTrack
                ?: session.player.currentMediaItem?.toTrackFallback()
            if (currentTrack == null || currentTrack.videoId.isBlank()) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
            }

            val targetLiked = !app.playbackController.isTrackLiked(currentTrack.videoId)
            return futureFromSuspend {
                runCatching {
                    ensureAuthReady()
                    val newLiked = repository.setLikeStatus(currentTrack.videoId, targetLiked)
                    withContext(Dispatchers.Main.immediate) {
                        app.playbackController.updateTrackLikedState(currentTrack.videoId, newLiked)
                        updateLikeCustomLayout(newLiked)
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }.getOrElse {
                    SessionResult(SessionError.ERROR_UNKNOWN)
                }
            }
        }

        @OptIn(UnstableApi::class)
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle(params?.extras ?: Bundle.EMPTY).apply {
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                )
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                )
            }
            val rootParams = LibraryParams.Builder()
                .setOffline(params?.isOffline ?: false)
                .setRecent(params?.isRecent ?: false)
                .setSuggested(params?.isSuggested ?: false)
                .setExtras(rootExtras)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(buildRootMediaItem(), rootParams))
        }

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = futureFromSuspend {
            runCatching {
                when {
                    parentId == ROOT_ID -> {
                        val tabs = listOf(
                            buildHomeTabMediaItem(),
                            buildPlaylistsTabMediaItem(),
                            buildDownloadedTabMediaItem(),
                        )
                        LibraryResult.ofItemList(paginate(tabs, page, pageSize), params)
                    }

                    parentId == TAB_HOME_ID -> {
                        ensureAuthReady()
                        val homeFeed = repository.fetchHomeFeed()
                        val shelves = homeFeed.shelves
                        val homeTracks = mutableListOf<Track>()
                        val items = mutableListOf<MediaItem>()

                        shelves.forEach { shelf ->
                            shelf.items.forEach { card ->
                                when (card) {
                                    is HomeCardItem.TrackCard -> {
                                        trackCache[card.track.videoId] = card.track
                                        homeTracks += card.track
                                        items += card.track.toPlayableMediaItem(
                                            sourceTag = TAB_HOME_ID,
                                            groupTitle = shelf.title,
                                        )
                                    }
                                    is HomeCardItem.CollectionCard -> {
                                        val summary = PlaylistSummary(
                                            id = card.id,
                                            title = card.title,
                                            subtitle = card.subtitle,
                                            thumbnailUrl = card.thumbnailUrl,
                                        )
                                        playlistCache[card.id] = summary
                                        items += card.toBrowsableMediaItem(groupTitle = shelf.title)
                                    }
                                }
                            }
                        }

                        homeTracksCache = homeTracks.distinctBy { it.videoId }
                        val dedupedItems = items.distinctBy { it.mediaId }
                        LibraryResult.ofItemList(paginate(dedupedItems, page, pageSize), params)
                    }

                    parentId == TAB_PLAYLISTS_ID -> {
                        ensureAuthReady()
                        if (!repository.isSignedIn()) {
                            return@runCatching LibraryResult.ofItemList(
                                paginate(listOf(buildSignInNoticeMediaItem()), page, pageSize),
                                params,
                            )
                        }
                        val playlists = repository.fetchLibraryPlaylists()
                        playlists.forEach { playlist ->
                            playlistCache[playlist.id] = playlist
                        }
                        val items = playlists.map { it.toBrowsableMediaItem() }
                        LibraryResult.ofItemList(paginate(items, page, pageSize), params)
                    }

                    parentId.startsWith(PLAYLIST_PREFIX) -> {
                        ensureAuthReady()
                        val playlistId = parentId.removePrefix(PLAYLIST_PREFIX)
                        val tracks = getOrFetchPlaylistTracks(playlistId)
                        val items = tracks.map {
                            it.toPlayableMediaItem(playlistId = playlistId, sourceTag = parentId)
                        }
                        LibraryResult.ofItemList(paginate(items, page, pageSize), params)
                    }

                    parentId == TAB_DOWNLOADED_ID -> {
                        if (downloadManager.downloadedTracks.value.isEmpty()) {
                            downloadManager.refreshExistingDownloads()
                        }
                        val tracks = downloadManager.downloadedTracks.value.map { it.toTrack() }
                        downloadedTracksCache = tracks
                        tracks.forEach { track -> trackCache[track.videoId] = track }
                        val items = tracks.map {
                            it.toPlayableMediaItem(sourceTag = TAB_DOWNLOADED_ID)
                        }
                        LibraryResult.ofItemList(paginate(items, page, pageSize), params)
                    }

                    else -> LibraryResult.ofError(
                        SessionError(SessionError.ERROR_BAD_VALUE, "Unknown folder: $parentId"),
                    )
                }
            }.getOrElse { err ->
                LibraryResult.ofError(
                    SessionError(
                        SessionError.ERROR_IO,
                        err.localizedMessage ?: "Failed to load library items",
                    ),
                )
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = futureFromSuspend {
            when {
                mediaId == ROOT_ID -> LibraryResult.ofItem(buildRootMediaItem(), null)
                mediaId == TAB_HOME_ID -> LibraryResult.ofItem(buildHomeTabMediaItem(), null)
                mediaId == TAB_PLAYLISTS_ID -> LibraryResult.ofItem(buildPlaylistsTabMediaItem(), null)
                mediaId == TAB_DOWNLOADED_ID -> LibraryResult.ofItem(buildDownloadedTabMediaItem(), null)
                mediaId == INFO_SIGN_IN_ID -> LibraryResult.ofItem(buildSignInNoticeMediaItem(), null)
                mediaId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlistId = mediaId.removePrefix(PLAYLIST_PREFIX)
                    val cached = playlistCache[playlistId]
                        ?: runCatching {
                            ensureAuthReady()
                            repository.fetchLibraryPlaylists().firstOrNull { it.id == playlistId }
                        }.getOrNull()
                    if (cached != null) {
                        playlistCache[cached.id] = cached
                        LibraryResult.ofItem(cached.toBrowsableMediaItem(), null)
                    } else {
                        LibraryResult.ofError(
                            SessionError(SessionError.ERROR_BAD_VALUE, "Playlist not found: $mediaId"),
                        )
                    }
                }
                else -> {
                    val track = trackCache[mediaId]
                        ?: downloadManager.downloadedTracks.value
                            .firstOrNull { it.videoId == mediaId }
                            ?.toTrack()
                    if (track != null) {
                        trackCache[track.videoId] = track
                        LibraryResult.ofItem(track.toPlayableMediaItem(), null)
                    } else {
                        LibraryResult.ofError(
                            SessionError(SessionError.ERROR_BAD_VALUE, "Track not found: $mediaId"),
                        )
                    }
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = futureFromSuspend {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                session.notifySearchResultChanged(browser, query, 0, params)
                return@futureFromSuspend LibraryResult.ofVoid(params)
            }
            runCatching {
                ensureAuthReady()
                val tracks = searchSongsViaRepository(trimmed)
                searchResultsCache[trimmed] = tracks
                tracks.forEach { track -> trackCache[track.videoId] = track }
                session.notifySearchResultChanged(browser, query, tracks.size, params)
                LibraryResult.ofVoid(params)
            }.getOrElse { err ->
                LibraryResult.ofError(
                    SessionError(
                        SessionError.ERROR_IO,
                        err.localizedMessage ?: "Search failed",
                    ),
                )
            }
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = futureFromSuspend {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return@futureFromSuspend LibraryResult.ofItemList(emptyList(), params)
            }
            runCatching {
                ensureAuthReady()
                val tracks = searchResultsCache[trimmed] ?: searchSongsViaRepository(trimmed).also { found ->
                    searchResultsCache[trimmed] = found
                    found.forEach { track -> trackCache[track.videoId] = track }
                }
                val items = tracks.map {
                    it.toPlayableMediaItem(searchQueryTag = trimmed)
                }
                LibraryResult.ofItemList(paginate(items, page, pageSize), params)
            }.getOrElse { err ->
                LibraryResult.ofError(
                    SessionError(
                        SessionError.ERROR_IO,
                        err.localizedMessage ?: "Failed to load search results",
                    ),
                )
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = futureFromSuspend {
            resolveRequestedMediaItems(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = futureFromSuspend {
            ensureAuthReady()
            val safeStartPosition = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs.coerceAtLeast(0L)

            if (mediaItems.size == 1) {
                val single = mediaItems.first()
                val mediaId = single.mediaId
                val extras = single.mediaMetadata.extras
                val requestSearchQuery = single.requestMetadata.searchQuery?.trim()

                if (mediaId.startsWith(PLAYLIST_PREFIX)) {
                    val playlistId = mediaId.removePrefix(PLAYLIST_PREFIX)
                    val tracks = getOrFetchPlaylistTracks(playlistId)
                    val items = tracks.map {
                        it.toPlayableMediaItem(playlistId = playlistId, sourceTag = mediaId)
                    }
                    syncQueueWithPlaybackController(tracks, 0)
                    return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, 0, safeStartPosition)
                }

                if (mediaId.isBlank() && !requestSearchQuery.isNullOrBlank()) {
                    val tracks = searchSongsViaRepository(requestSearchQuery)
                    searchResultsCache[requestSearchQuery] = tracks
                    tracks.forEach { trackCache[it.videoId] = it }
                    val items = tracks.map { it.toPlayableMediaItem(searchQueryTag = requestSearchQuery) }
                    syncQueueWithPlaybackController(tracks, 0)
                    return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, 0, safeStartPosition)
                }

                val playlistId = extras?.getString(EXTRA_PLAYLIST_ID)
                if (!playlistId.isNullOrBlank()) {
                    val tracks = getOrFetchPlaylistTracks(playlistId)
                    if (tracks.isNotEmpty()) {
                        val items = tracks.map {
                            it.toPlayableMediaItem(playlistId = playlistId, sourceTag = "$PLAYLIST_PREFIX$playlistId")
                        }
                        val idx = tracks.indexOfFirst { it.videoId == mediaId }.coerceAtLeast(0)
                        syncQueueWithPlaybackController(tracks, idx)
                        return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, idx, safeStartPosition)
                    }
                }

                val sourceTag = extras?.getString(EXTRA_SOURCE_TAG)
                if (sourceTag == TAB_HOME_ID) {
                    val tracks = getOrFetchHomeTracks()
                    if (tracks.isNotEmpty()) {
                        val items = tracks.map { it.toPlayableMediaItem(sourceTag = TAB_HOME_ID) }
                        val idx = tracks.indexOfFirst { it.videoId == mediaId }.coerceAtLeast(0)
                        syncQueueWithPlaybackController(tracks, idx)
                        return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, idx, safeStartPosition)
                    }
                }

                if (sourceTag == TAB_DOWNLOADED_ID) {
                    val tracks = downloadManager.downloadedTracks.value
                        .map { it.toTrack() }
                        .ifEmpty { downloadedTracksCache }
                    if (tracks.isNotEmpty()) {
                        val items = tracks.map { it.toPlayableMediaItem(sourceTag = TAB_DOWNLOADED_ID) }
                        val idx = tracks.indexOfFirst { it.videoId == mediaId }.coerceAtLeast(0)
                        syncQueueWithPlaybackController(tracks, idx)
                        return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, idx, safeStartPosition)
                    }
                }

                val searchTag = extras?.getString(EXTRA_SEARCH_QUERY)
                if (!searchTag.isNullOrBlank()) {
                    val tracks = searchResultsCache[searchTag].orEmpty()
                    if (tracks.isNotEmpty()) {
                        val items = tracks.map { it.toPlayableMediaItem(searchQueryTag = searchTag) }
                        val idx = tracks.indexOfFirst { it.videoId == mediaId }.coerceAtLeast(0)
                        syncQueueWithPlaybackController(tracks, idx)
                        return@futureFromSuspend MediaSession.MediaItemsWithStartPosition(items, idx, safeStartPosition)
                    }
                }
            }

            val resolved = resolveRequestedMediaItems(mediaItems)
            val safeIndex = if (resolved.isEmpty()) {
                0
            } else {
                startIndex.coerceIn(0, resolved.lastIndex)
            }
            if (resolved.isNotEmpty()) {
                val resolvedTracks = resolved.map { item ->
                    trackCache[item.mediaId] ?: item.toTrackFallback()
                }
                syncQueueWithPlaybackController(resolvedTracks, safeIndex)
            }
            MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, safeStartPosition)
        }

        private suspend fun syncQueueWithPlaybackController(tracks: List<Track>, startIndex: Int) {
            if (tracks.isEmpty()) return
            withContext(Dispatchers.Main.immediate) {
                app.playbackController.playTracksFromAuto(tracks, startIndex)
            }
        }

        private suspend fun searchSongsViaRepository(query: String): List<Track> =
            repository.search(query, SearchFilterOption.SONGS)
                .mapNotNull { (it as? SearchResultItem.SongResult)?.track }

        private suspend fun getOrFetchHomeTracks(): List<Track> {
            val cached = homeTracksCache
            if (cached.isNotEmpty()) return cached
            val homeFeed = repository.fetchHomeFeed()
            val shelves = homeFeed.shelves
            val tracks = shelves.flatMap { shelf ->
                shelf.items.mapNotNull { (it as? HomeCardItem.TrackCard)?.track }
            }.distinctBy { it.videoId }
            tracks.forEach { trackCache[it.videoId] = it }
            homeTracksCache = tracks
            return tracks
        }

        private suspend fun resolveRequestedMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
            ensureAuthReady()
            val resolved = mutableListOf<MediaItem>()
            for (item in mediaItems) {
                val id = item.mediaId
                val searchQuery = item.requestMetadata.searchQuery?.trim()
                when {
                    id.startsWith(PLAYLIST_PREFIX) -> {
                        val playlistId = id.removePrefix(PLAYLIST_PREFIX)
                        val tracks = getOrFetchPlaylistTracks(playlistId)
                        resolved += tracks.map {
                            it.toPlayableMediaItem(playlistId = playlistId, sourceTag = id)
                        }
                    }
                    id.isBlank() && !searchQuery.isNullOrBlank() -> {
                        val tracks = searchSongsViaRepository(searchQuery)
                        searchResultsCache[searchQuery] = tracks
                        tracks.forEach { trackCache[it.videoId] = it }
                        resolved += tracks.map { it.toPlayableMediaItem(searchQueryTag = searchQuery) }
                    }
                    id.isNotBlank() -> {
                        val cached = trackCache[id]
                        if (cached != null) {
                            resolved += cached.toPlayableMediaItem(
                                playlistId = item.mediaMetadata.extras?.getString(EXTRA_PLAYLIST_ID),
                                sourceTag = item.mediaMetadata.extras?.getString(EXTRA_SOURCE_TAG),
                                searchQueryTag = item.mediaMetadata.extras?.getString(EXTRA_SEARCH_QUERY),
                            )
                        } else {
                            val localFile = downloadManager.getDownloadedFile(id)
                            val targetUri = if (localFile != null && localFile.exists() && localFile.length() > 0L) {
                                Uri.fromFile(localFile)
                            } else {
                                item.localConfiguration?.uri ?: buildMediaUri(id)
                            }
                            resolved += item.buildUpon()
                                .setUri(targetUri)
                                .build()
                        }
                    }
                }
            }
            return resolved
        }

        private suspend fun getOrFetchPlaylistTracks(playlistId: String): List<Track> {
            playlistTracksCache[playlistId]?.let { cached ->
                if (cached.isNotEmpty()) return cached
            }
            val fetched = repository.fetchPlaylistTracks(playlistId).tracks
            playlistTracksCache[playlistId] = fetched
            fetched.forEach { track -> trackCache[track.videoId] = track }
            return fetched
        }

        private suspend fun ensureAuthReady() {
            app.isAuthInitialized.first { it }
            if (!repository.isSignedIn()) {
                val saved = app.authStore.getCookie()
                if (!saved.isNullOrBlank()) {
                    repository.setCookie(saved)
                }
            }
        }

        private fun <T> futureFromSuspend(block: suspend () -> T): ListenableFuture<T> {
            val future = SettableFuture.create<T>()
            serviceScope.launch {
                try {
                    future.set(block())
                } catch (t: Throwable) {
                    future.setException(t)
                }
            }
            return future
        }

        private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): List<T> {
            if (items.isEmpty() || page < 0 || pageSize <= 0) return emptyList()
            if (page == 0 && pageSize >= items.size) return items
            val fromIndex = page.toLong() * pageSize.toLong()
            if (fromIndex >= items.size) return emptyList()
            val toIndex = minOf(items.size.toLong(), fromIndex + pageSize.toLong()).toInt()
            return items.subList(fromIndex.toInt(), toIndex)
        }

        private fun buildRootMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("RazorTune")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()

        private fun buildHomeTabMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(TAB_HOME_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Home")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()

        private fun buildPlaylistsTabMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(TAB_PLAYLISTS_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Playlists")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()

        private fun buildDownloadedTabMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(TAB_DOWNLOADED_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Downloaded")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()

        private fun buildSignInNoticeMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(INFO_SIGN_IN_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Sign in on phone to sync playlists")
                        .setSubtitle("Open RazorTune > Settings > Account & Sync")
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build(),
                )
                .build()

        private fun PlaylistSummary.toBrowsableMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId("$PLAYLIST_PREFIX$id")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(songCountText ?: subtitle)
                        .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build(),
                )
                .build()

        @OptIn(UnstableApi::class)
        private fun HomeCardItem.CollectionCard.toBrowsableMediaItem(groupTitle: String? = null): MediaItem {
            val extras = Bundle().apply {
                groupTitle?.let { putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it) }
            }
            return MediaItem.Builder()
                .setMediaId("$PLAYLIST_PREFIX$id")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(
                            if (isAlbum) MediaMetadata.MEDIA_TYPE_ALBUM else MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        )
                        .setExtras(extras)
                        .build(),
                )
                .build()
        }

        @OptIn(UnstableApi::class)
        private fun Track.toPlayableMediaItem(
            playlistId: String? = null,
            sourceTag: String? = null,
            searchQueryTag: String? = null,
            groupTitle: String? = null,
        ): MediaItem {
            val extras = Bundle().apply {
                playlistId?.let { putString(EXTRA_PLAYLIST_ID, it) }
                sourceTag?.let { putString(EXTRA_SOURCE_TAG, it) }
                searchQueryTag?.let { putString(EXTRA_SEARCH_QUERY, it) }
                groupTitle?.let { putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it) }
            }
            val localFile = downloadManager.getDownloadedFile(videoId)
            val resolvedUri = if (localFile != null && localFile.exists() && localFile.length() > 0L) {
                Uri.fromFile(localFile)
            } else {
                buildMediaUri(videoId)
            }
            return MediaItem.Builder()
                .setMediaId(videoId)
                .setUri(resolvedUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setExtras(extras)
                        .build(),
                )
                .build()
        }

        private fun MediaItem.toTrackFallback(): Track {
            val meta = mediaMetadata
            return Track(
                videoId = mediaId,
                title = meta.title?.toString().orEmpty().ifBlank { "Untitled Track" },
                artist = meta.artist?.toString().orEmpty().ifBlank { "YouTube Music" },
                album = meta.albumTitle?.toString(),
                thumbnailUrl = meta.artworkUri?.toString(),
            )
        }
    }

    companion object {
        const val SCHEME_RAZORTUNE = "razortune"
        private const val YT_MUSIC_ORIGIN = "https://music.youtube.com"
        private const val YT_MUSIC_REFERER = "https://music.youtube.com/"
        private const val DEFAULT_YT_MUSIC_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private const val ROOT_ID = "root"
        private const val TAB_HOME_ID = "tab_home"
        private const val TAB_PLAYLISTS_ID = "tab_playlists"
        private const val TAB_DOWNLOADED_ID = "tab_downloaded"
        private const val INFO_SIGN_IN_ID = "info_sign_in"
        private const val PLAYLIST_PREFIX = "playlist:"
        private const val EXTRA_PLAYLIST_ID = "playlist_id"
        private const val EXTRA_SOURCE_TAG = "source_tag"
        private const val EXTRA_SEARCH_QUERY = "search_query"
        const val ACTION_TOGGLE_LIKE = "com.example.razortune.ACTION_TOGGLE_LIKE"

        fun buildMediaUri(videoId: String): Uri =
            Uri.parse("$SCHEME_RAZORTUNE://$videoId")
    }
}
