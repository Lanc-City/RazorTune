package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AudioQualityMode(
    val label: String,
    val shortLabel: String,
    val description: String,
) {
    HIGH(
        label = "High (Best Quality)",
        shortLabel = "High",
        description = "Selects the highest bitrate audio stream for maximum clarity.",
    ),
    BALANCED(
        label = "Balanced (128–160 kbps)",
        shortLabel = "Balanced",
        description = "Targets 128–160 kbps streams to cut data and battery drain.",
    ),
    SAVER(
        label = "Battery & Data Saver",
        shortLabel = "Saver",
        description = "Uses the lowest bitrate audio-only stream for longest playback.",
    ),
}

@Immutable
data class PerformanceSettings(
    val audioQuality: AudioQualityMode = AudioQualityMode.HIGH,
    val skipSilence: Boolean = false,
    val preloadNextTrack: Boolean = true,
    val reduceVisualEffects: Boolean = false,
    val autoReduceOnLowBattery: Boolean = true,
)

private val Context.performanceDataStore by preferencesDataStore(name = "razortune_performance")

class PerformancePreferencesStore(private val context: Context) {

    private object Keys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val PRELOAD_NEXT_TRACK = booleanPreferencesKey("preload_next_track")
        val REDUCE_VISUAL_EFFECTS = booleanPreferencesKey("reduce_visual_effects")
        val AUTO_REDUCE_ON_LOW_BATTERY = booleanPreferencesKey("auto_reduce_on_low_battery")
    }

    val settingsFlow: Flow<PerformanceSettings> = context.performanceDataStore.data.map { prefs ->
        val quality = prefs[Keys.AUDIO_QUALITY]?.let { name ->
            runCatching { AudioQualityMode.valueOf(name) }.getOrNull()
        } ?: AudioQualityMode.HIGH

        PerformanceSettings(
            audioQuality = quality,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            preloadNextTrack = prefs[Keys.PRELOAD_NEXT_TRACK] ?: true,
            reduceVisualEffects = prefs[Keys.REDUCE_VISUAL_EFFECTS] ?: false,
            autoReduceOnLowBattery = prefs[Keys.AUTO_REDUCE_ON_LOW_BATTERY] ?: true,
        )
    }

    suspend fun setAudioQuality(mode: AudioQualityMode) {
        context.performanceDataStore.edit { prefs ->
            prefs[Keys.AUDIO_QUALITY] = mode.name
        }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        context.performanceDataStore.edit { prefs ->
            prefs[Keys.SKIP_SILENCE] = enabled
        }
    }

    suspend fun setPreloadNextTrack(enabled: Boolean) {
        context.performanceDataStore.edit { prefs ->
            prefs[Keys.PRELOAD_NEXT_TRACK] = enabled
        }
    }

    suspend fun setReduceVisualEffects(enabled: Boolean) {
        context.performanceDataStore.edit { prefs ->
            prefs[Keys.REDUCE_VISUAL_EFFECTS] = enabled
        }
    }

    suspend fun setAutoReduceOnLowBattery(enabled: Boolean) {
        context.performanceDataStore.edit { prefs ->
            prefs[Keys.AUTO_REDUCE_ON_LOW_BATTERY] = enabled
        }
    }
}
