package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.model.DownloadStatus
import com.example.model.DownloadedTrack
import com.example.model.Track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request

class OfflineDownloadManager @JvmOverloads constructor(
    private val context: Context,
    private val repository: Any? = null,
    @Suppress("unused") private val storageStore: Any? = null
) {
    private val appContext = context.applicationContext
    private val offlineDir = File(appContext.filesDir, "audio_downloads").apply { if (!exists()) mkdirs() }
    private val metaDir = File(appContext.filesDir, "download_metadata").apply { if (!exists()) mkdirs() }
    private val legacyDir = File(appContext.filesDir, "downloads")
    private val tempDir = File(appContext.filesDir, "temp_cache").apply { if (!exists()) mkdirs() }

    private val downloadSemaphore = Semaphore(2)
    private val fileMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(10, 60, TimeUnit.SECONDS))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Using read-only getters to bypass interface type argument conflicts entirely
    private val _downloadStates = MutableStateFlow(emptyMap<String, DownloadStatus>())
    val downloadStates get() = _downloadStates.asStateFlow()

    private val _downloadProgress = MutableStateFlow(emptyMap<String, Float>())
    val downloadProgress get() = _downloadProgress.asStateFlow()

    private val _downloadedTracks = MutableStateFlow(emptyList<DownloadedTrack>())
    val downloadedTracks get() = _downloadedTracks.asStateFlow()

    private val _totalDownloadedBytes = MutableStateFlow(0L)
    val totalDownloadedBytes get() = _totalDownloadedBytes.asStateFlow()

    init {
        migrateLegacyDownloads()
        refreshExistingDownloads()
    }

    private fun migrateLegacyDownloads() {
        try {
            if (legacyDir.exists() && legacyDir.isDirectory) {
                legacyDir.listFiles()?.forEach { file ->
                    val target = File(offlineDir, file.name)
                    if (!target.exists()) {
                        file.renameTo(target)
                    } else {
                        file.delete()
                    }
                }
                legacyDir.deleteRecursively()
            }
        } catch (_: Exception) {
            Log.e("OfflineDownloadManager", "Legacy download migration failed")
        }
    }

    fun refreshExistingDownloads() {
        try {
            val tracks = mutableListOf<DownloadedTrack>()
            val statesMap = mutableMapOf<String, DownloadStatus>()
            var totalBytes = 0L

            offlineDir.listFiles()?.forEach { audioFile ->
                if (audioFile.isFile && audioFile.length() > 0L) {
                    val videoId = audioFile.nameWithoutExtension
                    statesMap[videoId] = DownloadStatus.DOWNLOADED
                    totalBytes += audioFile.length()

                    val metaFile = File(metaDir, "$videoId.json")
                    val metadata = if (metaFile.exists()) {
                        try {
                            json.decodeFromString<DownloadedTrack>(metaFile.readText())
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val downloadedTrack = metadata ?: createDownloadedTrack(
                        videoId = videoId,
                        title = "Downloaded Track ($videoId)",
                        artist = "Unknown Artist",
                        duration = 0,
                        thumbnailUrl = "",
                        fileSize = audioFile.length(),
                        timestamp = audioFile.lastModified()
                    )
                    tracks.add(downloadedTrack)
                }
            }

            tracks.sortByDescending { getTrackTimestamp(it) }
            _downloadedTracks.value = tracks
            _downloadStates.value = statesMap
            _totalDownloadedBytes.value = totalBytes
        } catch (e: Exception) {
            Log.e("OfflineDownloadManager", "Failed to refresh existing downloads", e)
        }
    }

    fun getStatus(videoId: String): DownloadStatus =
        _downloadStates.value[videoId] ?: DownloadStatus.NOT_DOWNLOADED

    fun getDownloadedFile(videoId: String): File? {
        val m4aFile = File(offlineDir, "$videoId.m4a")
        if (m4aFile.exists() && m4aFile.length() > 0L) return m4aFile
        val legacyFile = File(offlineDir, videoId)
        if (legacyFile.exists() && legacyFile.length() > 0L) return legacyFile
        return null
    }

    private suspend fun getTrackStreamUrl(track: Track): String? {
        return try {
            val repo = repository as? InnerTubeRepository ?: return null
            val stream = repo.resolveAudioStream(track.videoId)
            stream.audioUrl
        } catch (e: Exception) {
            Log.e("OfflineDownloadManager", "Failed to resolve audio stream for ${track.videoId}", e)
            null
        }
    }

    private suspend fun downloadTrackInternal(track: Track): Result<Pair<DownloadedTrack, File>> = withContext(Dispatchers.IO) {
        val trackId = getTrackId(track)
        val trackTitle = getTrackTitle(track)
        val trackUrl = getTrackStreamUrl(track)

        if (_downloadStates.value[trackId] == DownloadStatus.DOWNLOADING) {
            return@withContext Result.failure(IllegalStateException("Already downloading \"$trackTitle\""))
        }

        downloadSemaphore.withPermit {
            _downloadStates.update { map -> map + (trackId to DownloadStatus.DOWNLOADING) }
            _downloadProgress.update { map -> map + (trackId to 0f) }

            var attempt = 0
            val maxAttempts = 3
            var lastException: Throwable? = null

            while (attempt < maxAttempts) {
                attempt++
                val targetFile = File(offlineDir, "$trackId.m4a")
                val tempFile = File(tempDir, "$trackId.tmp")

                try {
                    val streamUrl = trackUrl ?: run {
                        throw IOException("No audio stream URL available for track")
                    }

                    val headRequest = Request.Builder().url(streamUrl).head().build()
                    val totalLength = httpClient.newCall(headRequest).execute().use { res ->
                        res.header("Content-Length")?.toLongOrNull() ?: 0L
                    }

                    val chunkSize = 1_048_576L
                    var downloadedBytes = 0L
                    tempFile.delete()

                    FileOutputStream(tempFile, true).use { output ->
                        while (totalLength <= 0L || downloadedBytes < totalLength) {
                            val endRange = if (totalLength > 0L) {
                                minOf(downloadedBytes + chunkSize - 1L, totalLength - 1L)
                            } else {
                                downloadedBytes + chunkSize - 1L
                            }

                            val chunkRequest = Request.Builder()
                                .url(streamUrl)
                                .header("Range", "bytes=$downloadedBytes-$endRange")
                                .get()
                                .build()

                            httpClient.newCall(chunkRequest).execute().use { response ->
                                if (!response.isSuccessful && response.code != 206 && response.code != 200) {
                                    error("Chunk download failed with HTTP ${response.code}")
                                }

                                val body = response.body ?: error("Empty response body")
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read

                                        if (totalLength > 0L) {
                                            val progress = (downloadedBytes.toFloat() / totalLength.toFloat()).coerceIn(0f, 0.99f)
                                            _downloadProgress.update { map -> map + (trackId to progress) }
                                        }
                                    }
                                }
                            }

                            if (totalLength <= 0L || downloadedBytes >= totalLength) break
                        }
                        output.flush()
                    }

                    fileMutex.withAction {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)
                    }

                    val downloadedTrack = createDownloadedTrack(
                        videoId = trackId,
                        title = trackTitle,
                        artist = getTrackArtist(track),
                        duration = getTrackDuration(track),
                        thumbnailUrl = getTrackThumbnail(track),
                        fileSize = targetFile.length(),
                        timestamp = System.currentTimeMillis()
                    )

                    val metaFile = File(metaDir, "$trackId.json")
                    metaFile.writeText(json.encodeToString(DownloadedTrack.serializer(), downloadedTrack))

                    return@withContext Result.success(downloadedTrack to targetFile)
                } catch (e: Exception) {
                    lastException = e
                    Log.w("OfflineDownloadManager", "Attempt $attempt failed for $trackTitle: ${e.message}")
                    if (attempt < maxAttempts) {
                        delay((2000L * attempt).milliseconds)
                    }
                }
            }

            _downloadStates.update { map -> map + (trackId to DownloadStatus.FAILED) }
            _downloadProgress.update { map -> map.filterKeys { it != trackId } }
            return@withContext Result.failure(lastException ?: IOException("Download failed after $maxAttempts attempts"))
        }
    }

    fun downloadTrack(track: Track, onComplete: (Result<File>) -> Unit) {
        scope.launch {
            val trackId = getTrackId(track)
            val result = downloadTrackInternal(track)
            if (result.isSuccess) {
                val (downloadedTrack, targetFile) = result.getOrThrow()
                _downloadStates.update { map -> map + (trackId to DownloadStatus.DOWNLOADED) }
                _downloadProgress.update { map -> map + (trackId to 1f) }
                _downloadedTracks.update { current ->
                    val updated = listOf(downloadedTrack) + current.filter { getDownloadedTrackId(it) != trackId }
                    _totalDownloadedBytes.value = updated.sumOf { getDownloadedTrackSize(it) }
                    updated
                }
                onComplete(Result.success(targetFile))
            } else {
                onComplete(Result.failure(result.exceptionOrNull() ?: IOException("Download failed")))
            }
        }
    }

    fun downloadTracks(
        tracks: List<Track>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        onAllComplete: (successCount: Int, failedCount: Int) -> Unit = { _, _ -> }
    ) {
        scope.launch {
            val tracksToDownload = tracks.filter { getStatus(getTrackId(it)) != DownloadStatus.DOWNLOADED }
            var successCount = 0
            var failedCount = 0
            var completedCount = 0
            val total = tracksToDownload.size

            for (track in tracksToDownload) {
                val trackId = getTrackId(track)
                val result = downloadTrackInternal(track)
                if (result.isSuccess) {
                    val (downloadedTrack, _) = result.getOrThrow()
                    _downloadStates.update { map -> map + (trackId to DownloadStatus.DOWNLOADED) }
                    _downloadProgress.update { map -> map + (trackId to 1f) }
                    _downloadedTracks.update { current ->
                        val updated = listOf(downloadedTrack) + current.filter { getDownloadedTrackId(it) != trackId }
                        _totalDownloadedBytes.value = updated.sumOf { getDownloadedTrackSize(it) }
                        updated
                    }
                    successCount++
                } else {
                    failedCount++
                }
                completedCount++
                onProgress(completedCount, total)
            }
            onAllComplete(successCount, failedCount)
        }
    }

    fun removeDownload(videoId: String) {
        try {
            getDownloadedFile(videoId)?.delete()
            File(metaDir, "$videoId.json").delete()
            File(offlineDir, "$videoId.art").delete()

            _downloadStates.update { map -> map.filterKeys { it != videoId } }
            _downloadProgress.update { map -> map.filterKeys { it != videoId } }
            _downloadedTracks.update { list ->
                val updated = list.filter { getDownloadedTrackId(it) != videoId }
                _totalDownloadedBytes.value = updated.sumOf { getDownloadedTrackSize(it) }
                updated
            }
        } catch (e: Exception) {
            Log.e("OfflineDownloadManager", "Failed to remove download $videoId", e)
        }
    }

    fun deleteAllDownloads() {
        try {
            offlineDir.listFiles()?.forEach { it.delete() }
            metaDir.listFiles()?.forEach { it.delete() }
            legacyDir.listFiles()?.forEach { it.delete() }
            tempDir.listFiles()?.forEach { it.delete() }

            _downloadStates.value = emptyMap()
            _downloadProgress.value = emptyMap()
            _downloadedTracks.value = emptyList()
            _totalDownloadedBytes.value = 0L
        } catch (e: Exception) {
            Log.e("OfflineDownloadManager", "Failed to delete all downloads", e)
        }
    }

    fun calculateCacheSizeBytes(): Long {
        var size = 0L
        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) size += file.length()
            }
        } catch (_: Exception) {}
        return size
    }

    fun clearTemporaryCache(): Long {
        var freed = 0L
        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    freed += file.length()
                    file.delete()
                }
            }
        } catch (_: Exception) {}
        return freed
    }

    private suspend inline fun <T> Mutex.withAction(action: () -> T): T {
        lock()
        try {
            return action()
        } finally {
            unlock()
        }
    }

    private fun getTrackId(track: Track): String = track.videoId
    private fun getTrackTitle(track: Track): String = track.title
    private fun getTrackArtist(track: Track): String = track.artist
    private fun getTrackDuration(track: Track): Int = track.durationSeconds ?: 0
    private fun getTrackThumbnail(track: Track): String = track.thumbnailUrl ?: ""

    private fun createDownloadedTrack(
        videoId: String,
        title: String,
        artist: String,
        duration: Int,
        thumbnailUrl: String,
        fileSize: Long,
        timestamp: Long
    ): DownloadedTrack {
        return DownloadedTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            album = null,
            durationMs = duration.toLong() * 1000L,
            thumbnailUrl = thumbnailUrl,
            fileSizeBytes = fileSize,
            downloadedAtMs = timestamp
        )
    }

    private fun getDownloadedTrackId(item: DownloadedTrack): String = item.videoId
    private fun getDownloadedTrackSize(item: DownloadedTrack): Long = item.fileSizeBytes
    private fun getTrackTimestamp(item: DownloadedTrack): Long = item.downloadedAtMs

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.getDefault(), "%.2f GB", gb)
        }
    }
}