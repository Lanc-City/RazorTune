package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EqPreset(
    val label: String,
    val bandLevelsMb: List<Int>,
) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", listOf(600, 400, 0, 0, 100)),
    TRAP_HIPHOP("Trap & Dembow", listOf(750, 450, -100, 250, 500)),
    VOCAL_CLARITY("Vocal Clarity", listOf(-200, 100, 450, 500, 200)),
    SPATIAL_SURROUND("Spatial Surround", listOf(300, 100, 0, 350, 550)),
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0)),
}

@Immutable
data class AudioEffectsSettings(
    val enabled: Boolean = false,
    val preset: EqPreset = EqPreset.FLAT,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
)

private val Context.audioEffectsDataStore by preferencesDataStore(name = "razortune_audio_effects")

class AudioEffectsPreferencesStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("audio_effects_enabled")
        val PRESET = stringPreferencesKey("audio_effects_preset")
        val BAND_LEVELS_MB = stringPreferencesKey("audio_effects_band_levels_mb")
        val BASS_BOOST_STRENGTH = intPreferencesKey("audio_effects_bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("audio_effects_virtualizer_strength")
        val LOUDNESS_GAIN_MB = intPreferencesKey("audio_effects_loudness_gain_mb")
    }

    val settingsFlow: Flow<AudioEffectsSettings> = context.audioEffectsDataStore.data.map { prefs ->
        val preset = prefs[Keys.PRESET]?.let { name ->
            runCatching { EqPreset.valueOf(name) }.getOrNull()
        } ?: EqPreset.FLAT

        val bandLevels = decodeBandLevels(prefs[Keys.BAND_LEVELS_MB], preset.bandLevelsMb)

        AudioEffectsSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            preset = preset,
            bandLevelsMb = bandLevels,
            bassBoostStrength = (prefs[Keys.BASS_BOOST_STRENGTH] ?: 0).coerceIn(0, 1000),
            virtualizerStrength = (prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0).coerceIn(0, 1000),
            loudnessGainMb = (prefs[Keys.LOUDNESS_GAIN_MB] ?: 0).coerceIn(0, 1000),
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
        }
    }

    suspend fun selectPreset(preset: EqPreset) {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.PRESET] = preset.name
            if (preset != EqPreset.CUSTOM) {
                prefs[Keys.BAND_LEVELS_MB] = encodeBandLevels(preset.bandLevelsMb)
            }
        }
    }

    suspend fun setBandLevel(bandIndex: Int, levelMb: Int) {
        if (bandIndex !in 0 until BAND_COUNT) return
        val clamped = levelMb.coerceIn(MIN_BAND_MB, MAX_BAND_MB)
        context.audioEffectsDataStore.edit { prefs ->
            val currentPreset = prefs[Keys.PRESET]?.let { name ->
                runCatching { EqPreset.valueOf(name) }.getOrNull()
            } ?: EqPreset.FLAT
            val current = decodeBandLevels(prefs[Keys.BAND_LEVELS_MB], currentPreset.bandLevelsMb).toMutableList()
            current[bandIndex] = clamped
            prefs[Keys.PRESET] = EqPreset.CUSTOM.name
            prefs[Keys.BAND_LEVELS_MB] = encodeBandLevels(current)
        }
    }

    suspend fun setBassBoost(strength: Int) {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.BASS_BOOST_STRENGTH] = strength.coerceIn(0, 1000)
        }
    }

    suspend fun setVirtualizer(strength: Int) {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.VIRTUALIZER_STRENGTH] = strength.coerceIn(0, 1000)
        }
    }

    suspend fun setLoudnessGain(gainMb: Int) {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.LOUDNESS_GAIN_MB] = gainMb.coerceIn(0, 1000)
        }
    }

    suspend fun resetAll() {
        context.audioEffectsDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = false
            prefs[Keys.PRESET] = EqPreset.FLAT.name
            prefs[Keys.BAND_LEVELS_MB] = encodeBandLevels(EqPreset.FLAT.bandLevelsMb)
            prefs[Keys.BASS_BOOST_STRENGTH] = 0
            prefs[Keys.VIRTUALIZER_STRENGTH] = 0
            prefs[Keys.LOUDNESS_GAIN_MB] = 0
        }
    }

    private fun encodeBandLevels(levels: List<Int>): String =
        levels.take(BAND_COUNT).joinToString(",")

    private fun decodeBandLevels(raw: String?, fallback: List<Int>): List<Int> {
        if (raw.isNullOrBlank()) return normalizeBandList(fallback)
        val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (parsed.size == BAND_COUNT) {
            normalizeBandList(parsed)
        } else {
            normalizeBandList(fallback)
        }
    }

    private fun normalizeBandList(levels: List<Int>): List<Int> =
        List(BAND_COUNT) { index ->
            (levels.getOrNull(index) ?: 0).coerceIn(MIN_BAND_MB, MAX_BAND_MB)
        }

    companion object {
        const val BAND_COUNT = 5
        const val MIN_BAND_MB = -1200
        const val MAX_BAND_MB = 1200
    }
}
