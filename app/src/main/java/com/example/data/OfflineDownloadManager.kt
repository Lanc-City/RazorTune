package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import coil.Coil
import com.example.model.DownloadStatus
import com.example.model.DownloadedTrack
import com.example.model.Track
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class OfflineDownloadManager(
    private val context: Context,
    private val repository: InnerTubeRepository,
    private val storageStore: StoragePreferencesStore,
) {
    private val offlineDir = File(context.filesDir, "offline_audio").apply { mkdirs() }
    private val legacyDir = File(context.filesDir, "downloads")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder().followRedirects(true).build()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val downloadSemaphore = Semaphore(2)

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<DownloadedTrack>>(emptyList())
    val downloadedTracks: StateFlow<List<DownloadedTrack>> = _downloadedTracks.asStateFlow()

    private val _totalDownloadedBytes = MutableStateFlow(0L)
    val totalDownloadedBytes: StateFlow<Long> = _totalDownloadedBytes.asStateFlow()

    init {
        scope.launch {
            migrateLegacyDownloads()
            refreshExistingDownloads()
        }
    }

    private fun migrateLegacyDownloads() {
        if (!legacyDir.exists() || !legacyDir.isDirectory) return
        val legacyFiles = legacyDir.listFiles() ?: return
        for (legacy in legacyFiles) {
            if (legacy.isFile && legacy.name.endsWith(".audio") && legacy.length() > 0L) {
                val videoId = legacy.name.removeSuffix(".audio")
                val targetFile = File(offlineDir, "$videoId.m4a")
                if (!targetFile.exists()) {
                    runCatching {
                        legacy.copyTo(targetFile, overwrite = true)
                        legacy.delete()
                    }
                }
            }
        }
    }

    fun refreshExistingDownloads() {
        val tracks = mutableListOf<DownloadedTrack>()
        var totalBytes = 0L
        val statesMap = mutableMapOf<String, DownloadStatus>()

        val audioFiles = offlineDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".m4a") || file.name.endsWith(".audio")) && file.length() > 0L
        } ?: emptyArray()

        for (audioFile in audioFiles) {
            val videoId = audioFile.name.substringBeforeLast(".")
            val metaFile = File(offlineDir, "$videoId.json")
            val fileSize = audioFile.length()
            totalBytes += fileSize
            statesMap[videoId] = DownloadStatus.DOWNLOADED

            var metadata: DownloadedTrack? = null
            if (metaFile.exists()) {
                metadata = runCatching {
                    json.decodeFromString<DownloadedTrack>(metaFile.readText())
                }.getOrNull()
            }

            val artFile = File(offlineDir, "$videoId.art")
            val localArtUri = if (artFile.exists() && artFile.length() > 0L) artFile.toURI().toString() else null

            if (metadata == null) {
                metadata = DownloadedTrack(
                    videoId = videoId,
                    title = "Downloaded Track ($videoId)",
                    artist = "Offline Audio",
                    album = null,
                    durationMs = 0L,
                    thumbnailUrl = localArtUri,
                    fileSizeBytes = fileSize,
                    downloadedAtMs = audioFile.lastModified(),
                )
            } else {
                metadata = metadata.copy(
                    fileSizeBytes = if (metadata.fileSizeBytes <= 0L) fileSize else metadata.fileSizeBytes,
                    thumbnailUrl = localArtUri ?: metadata.thumbnailUrl,
                )
            }
            tracks.add(metadata)
        }

        tracks.sortByDescending { it.downloadedAtMs }
        _downloadedTracks.value = tracks
        _downloadStates.value = statesMap
        _totalDownloadedBytes.value = totalBytes
    }

    fun getStatus(videoId: String): DownloadStatus =
        _downloadStates.value[videoId] ?: DownloadStatus.NOT_DOWNLOADED

    fun getDownloadedFile(videoId: String): File? {
        val m4aFile = File(offlineDir, "$videoId.m4a")
        if (m4aFile.exists() && m4aFile.length() > 0L) return m4aFile

        val legacyAudio = File(offlineDir, "$videoId.audio")
        if (legacyAudio.exists() && legacyAudio.length() > 0L) return legacyAudio

        val oldDirAudio = File(legacyDir, "$videoId.audio")
        if (oldDirAudio.exists() && oldDirAudio.length() > 0L) return oldDirAudio

        return null
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun downloadTrack(
        track: Track,
        onComplete: (Result<File>) -> Unit = {},
    ) {
        if (_downloadStates.value[track.videoId] == DownloadStatus.DOWNLOADING) {
            onComplete(Result.failure(IllegalStateException("Already downloading \"${track.title}\"")))
            return
        }

        scope.launch {
            val wifiOnly = storageStore.downloadOverWifiOnlyFlow.first()
            if (wifiOnly && !isWifiConnected()) {
                val error = IllegalStateException("Wi-Fi required: 'Download over Wi-Fi only' is enabled.")
                _downloadStates.update { it + (track.videoId to DownloadStatus.FAILED) }
                withContext(Dispatchers.Main.immediate) {
                    onComplete(Result.failure(error))
                }
                return@launch
            }

            downloadSemaphore.withPermit {
                _downloadStates.update { it + (track.videoId to DownloadStatus.DOWNLOADING) }
                _downloadProgress.update { it + (track.videoId to 0f) }

                val result = runCatching {
                    val stream = repository.resolveAudioStream(track.videoId)
                    val targetFile = File(offlineDir, "${track.videoId}.m4a")
                    val tempFile = File(offlineDir, "${track.videoId}.part")
                    val metaFile = File(offlineDir, "${track.videoId}.json")
                    val artFile = File(offlineDir, "${track.videoId}.art")

                    val requestBuilder = Request.Builder().url(stream.audioUrl)
                    requestBuilder.header("Origin", "https://music.youtube.com")
                    requestBuilder.header("Referer", "https://music.youtube.com/")
                    requestBuilder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                    )
                    stream.headers.forEach { (k, v) ->
                        if (v.isNotBlank()) requestBuilder.header(k, v)
                    }

                    httpClient.newCall(requestBuilder.build()).execute().use { response ->
                        if (!response.isSuccessful) {
                            error("Download failed with HTTP ${response.code}")
                        }
                        val body = response.body ?: error("Empty audio stream body")
                        val totalBytes = body.contentLength().coerceAtLeast(1L)
                        var bytesRead = 0L

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                val buffer = ByteArray(16 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    bytesRead += read
                                    if (totalBytes > 0) {
                                        val progress = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                        _downloadProgress.update { it + (track.videoId to progress) }
                                    }
                                }
                                output.flush()
                            }
                        }
                    }

                    if (targetFile.exists()) targetFile.delete()
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }

                    // Cache album art locally if available
                    if (!track.thumbnailUrl.isNullOrBlank()) {
                        runCatching {
                            val artReq = Request.Builder().url(track.thumbnailUrl).build()
                            httpClient.newCall(artReq).execute().use { artResp ->
                                if (artResp.isSuccessful) {
                                    artResp.body?.bytes()?.let { bytes ->
                                        artFile.writeBytes(bytes)
                                    }
                                }
                            }
                        }
                    }

                    val resolvedThumb = if (artFile.exists() && artFile.length() > 0L) {
                        artFile.toURI().toString()
                    } else {
                        track.thumbnailUrl
                    }

                    val downloadedTrack = DownloadedTrack(
                        videoId = track.videoId,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        durationMs = (track.durationSeconds ?: 0) * 1000L,
                        thumbnailUrl = resolvedThumb,
                        fileSizeBytes = targetFile.length(),
                        downloadedAtMs = System.currentTimeMillis(),
                    )
                    metaFile.writeText(json.encodeToString(DownloadedTrack.serializer(), downloadedTrack))

                    downloadedTrack to targetFile
                }

                if (result.isSuccess) {
                    val (downloadedTrack, targetFile) = result.getOrThrow()
                    _downloadStates.update { it + (track.videoId to DownloadStatus.DOWNLOADED) }
                    _downloadProgress.update { it - track.videoId }
                    _downloadedTracks.update { current ->
                        val updated = listOf(downloadedTrack) + current.filter { it.videoId != track.videoId }
                        _totalDownloadedBytes.value = updated.sumOf { it.fileSizeBytes }
                        updated
                    }

                    withContext(Dispatchers.Main.immediate) {
                        onComplete(Result.success(targetFile))
                    }
                } else {
                    File(offlineDir, "${track.videoId}.part").delete()
                    _downloadStates.update { it + (track.videoId to DownloadStatus.FAILED) }
                    _downloadProgress.update { it - track.videoId }

                    withContext(Dispatchers.Main.immediate) {
                        onComplete(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
                    }
                }
            }
        }
    }

    fun downloadTracks(
        tracks: List<Track>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        onAllComplete: (successCount: Int, failedCount: Int) -> Unit = { _, _ -> },
    ) {
        val tracksToDownload = tracks.filter { getStatus(it.videoId) != DownloadStatus.DOWNLOADED }
        if (tracksToDownload.isEmpty()) {
            onAllComplete(0, 0)
            return
        }

        scope.launch {
            var completedCount = 0
            var successCount = 0
            var failedCount = 0
            val total = tracksToDownload.size

            for (track in tracksToDownload) {
                downloadTrack(track) { res ->
                    completedCount++
                    if (res.isSuccess) successCount++ else failedCount++
                    onProgress(completedCount, total)
                    if (completedCount >= total) {
                        onAllComplete(successCount, failedCount)
                    }
                }
            }
        }
    }

    fun removeDownload(videoId: String) {
        scope.launch {
            val audio = getDownloadedFile(videoId)
            val fileSize = audio?.length() ?: 0L
            audio?.delete()
            File(offlineDir, "$videoId.m4a").delete()
            File(offlineDir, "$videoId.audio").delete()
            File(offlineDir, "$videoId.part").delete()
            File(offlineDir, "$videoId.json").delete()
            File(offlineDir, "$videoId.art").delete()

            _downloadStates.update { it - videoId }
            _downloadProgress.update { it - videoId }
            _downloadedTracks.update { list ->
                val updated = list.filter { it.videoId != videoId }
                _totalDownloadedBytes.value = updated.sumOf { it.fileSizeBytes }
                updated
            }
        }
    }

    fun deleteAllDownloads() {
        scope.launch {
            offlineDir.listFiles()?.forEach { it.delete() }
            legacyDir.listFiles()?.forEach { it.delete() }

            _downloadStates.value = emptyMap()
            _downloadProgress.value = emptyMap()
            _downloadedTracks.value = emptyList()
            _totalDownloadedBytes.value = 0L
        }
    }

    fun calculateCacheSizeBytes(): Long {
        var size = calculateDirSize(context.cacheDir)
        context.externalCacheDir?.let { size += calculateDirSize(it) }
        return size
    }

    fun clearTemporaryCache(): Long {
        val initialSize = calculateCacheSizeBytes()
        runCatching {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            Coil.imageLoader(context).diskCache?.clear()
            Coil.imageLoader(context).memoryCache?.clear()
        }
        return initialSize
    }

    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { file ->
            total += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return total
    }

    fun getDownloadedCount(): Int = _downloadedTracks.value.size

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / 1024.0.pow(digitGroups.toDouble())
            return "%.1f %s".format(value, units[digitGroups])
        }
    }
}
