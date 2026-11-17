package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.example.data.AudioEffectsPreferencesStore
import com.example.data.AudioEffectsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class RazorTuneAudioEffectsManager(
    private val store: AudioEffectsPreferencesStore,
) {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Any()

    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var latestSettings: AudioEffectsSettings = AudioEffectsSettings()
    private var settingsJob: Job? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        ensureSettingsCollection()
    }

    private fun ensureSettingsCollection() {
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
        if (settingsJob?.isActive == true) return
        settingsJob = scope.launch {
            store.settingsFlow.collect { settings ->
                synchronized(lock) {
                    latestSettings = settings
                    applySettingsLocked(settings)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        ensureSettingsCollection()

        synchronized(lock) {
            if (audioSessionId == currentSessionId && hasAnyActiveHandleLocked()) {
                applySettingsLocked(latestSettings)
                return
            }

            releaseEffectsLocked()
            currentSessionId = audioSessionId

            equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
            bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
            loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()

            applySettingsLocked(latestSettings)
        }
    }

    fun getCurrentAudioSessionId(): Int = synchronized(lock) { currentSessionId }

    fun detachSession() {
        synchronized(lock) {
            releaseEffectsLocked()
            currentSessionId = C.AUDIO_SESSION_ID_UNSET
        }
    }

    fun release() {
        settingsJob?.cancel()
        settingsJob = null
        detachSession()
        scope.cancel()
    }

    private fun hasAnyActiveHandleLocked(): Boolean =
        equalizer != null || bassBoost != null || virtualizer != null || loudnessEnhancer != null

    private fun releaseEffectsLocked() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
        runCatching { virtualizer?.release() }
        virtualizer = null
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
    }

    private fun applySettingsLocked(settings: AudioEffectsSettings) {
        val active = settings.enabled

        equalizer?.let { eq ->
            runCatching {
                eq.enabled = active
                val numBands = eq.numberOfBands.toInt()
                if (numBands > 0 && settings.bandLevelsMb.isNotEmpty()) {
                    val range = eq.bandLevelRange
                    val minMb = (range.getOrNull(0) ?: -1500).toInt()
                    val maxMb = (range.getOrNull(1) ?: 1500).toInt()

                    for (bandIdx in 0 until numBands) {
                        val sourceIdx = if (numBands == settings.bandLevelsMb.size) {
                            bandIdx
                        } else if (numBands <= 1) {
                            0
                        } else {
                            (bandIdx * (settings.bandLevelsMb.size - 1)) / (numBands - 1)
                        }
                        val targetMb = settings.bandLevelsMb
                            .getOrElse(sourceIdx) { 0 }
                            .coerceIn(minMb, maxMb)
                        eq.setBandLevel(bandIdx.toShort(), targetMb.toShort())
                    }
                }
            }
        }

        bassBoost?.let { boost ->
            runCatching {
                boost.enabled = active
                if (boost.strengthSupported) {
                    val strength = settings.bassBoostStrength.coerceIn(0, 1000).toShort()
                    boost.setStrength(strength)
                }
            }
        }

        virtualizer?.let { virt ->
            runCatching {
                virt.enabled = active
                if (virt.strengthSupported) {
                    val strength = settings.virtualizerStrength.coerceIn(0, 1000).toShort()
                    virt.setStrength(strength)
                }
            }
        }

        loudnessEnhancer?.let { enhancer ->
            runCatching {
                enhancer.enabled = active
                val gainMb = settings.loudnessGainMb.coerceIn(0, 1000)
                enhancer.setTargetGain(gainMb)
            }
        }
    }
}
