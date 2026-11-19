package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.AudioEffectsPreferencesStore
import com.example.data.AuthPreferencesStore
import com.example.data.InnerTubeRepository
import com.example.data.ListeningStatsStore
import com.example.data.OfflineDownloadManager
import com.example.data.PerformancePreferencesStore
import com.example.data.StoragePreferencesStore
import com.example.data.ThemePreferencesStore
import com.example.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RazorTuneApplication : Application(), ImageLoaderFactory {

    lateinit var authStore: AuthPreferencesStore
        private set

    lateinit var themeStore: ThemePreferencesStore
        private set

    lateinit var storageStore: StoragePreferencesStore
        private set

    lateinit var statsStore: ListeningStatsStore
        private set

    lateinit var performanceStore: PerformancePreferencesStore
        private set

    lateinit var audioEffectsStore: AudioEffectsPreferencesStore
        private set

    lateinit var repository: InnerTubeRepository
        private set

    val innerTubeRepository: InnerTubeRepository
        get() = repository

    lateinit var downloadManager: OfflineDownloadManager
        private set

    lateinit var playbackController: PlaybackController
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        authStore = AuthPreferencesStore(this)
        themeStore = ThemePreferencesStore(this)
        storageStore = StoragePreferencesStore(this)
        statsStore = ListeningStatsStore(this)
        performanceStore = PerformancePreferencesStore(this)
        audioEffectsStore = AudioEffectsPreferencesStore(this)
        repository = InnerTubeRepository()
        downloadManager = OfflineDownloadManager(this, repository, storageStore)
        playbackController = PlaybackController(this)

        appScope.launch {
            val savedCookie = authStore.getCookie()
            repository.setCookie(savedCookie)
            _isAuthInitialized.value = true
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .crossfade(150)
            .build()
    }

    companion object {
        @Volatile
        lateinit var instance: RazorTuneApplication
            private set
    }
}
