package com.example.playback

import android.media.audiofx.Visualizer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioVisualizerManager(
    private val barCount: Int = DEFAULT_BAR_COUNT,
) {
    private val lock = Any()
    private val zeroBars = List(barCount) { 0f }
    private val smoothedBars = FloatArray(barCount)

    private val _magnitudes = MutableStateFlow(zeroBars)
    val magnitudes: StateFlow<List<Float>> = _magnitudes.asStateFlow()

    @OptIn(UnstableApi::class)
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var visualizer: Visualizer? = null
    private var isActiveRequested: Boolean = false

    @OptIn(UnstableApi::class)
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return

        synchronized(lock) {
            if (audioSessionId == currentSessionId) {
                if (isActiveRequested && visualizer == null) {
                    enableVisualizerLocked(audioSessionId)
                }
                return
            }

            releaseVisualizerLocked()
            currentSessionId = audioSessionId

            if (isActiveRequested) {
                enableVisualizerLocked(audioSessionId)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun setActive(isActive: Boolean) {
        synchronized(lock) {
            isActiveRequested = isActive
            if (isActive && currentSessionId != C.AUDIO_SESSION_ID_UNSET && currentSessionId > 0) {
                if (visualizer == null) {
                    enableVisualizerLocked(currentSessionId)
                } else {
                    val enabled = runCatching {
                        visualizer?.enabled = true
                        true
                    }.getOrDefault(false)
                    if (!enabled) {
                        releaseVisualizerLocked()
                        resetMagnitudesLocked()
                    }
                }
            } else {
                runCatching { visualizer?.enabled = false }
                resetMagnitudesLocked()
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun detachSession() {
        synchronized(lock) {
            releaseVisualizerLocked()
            currentSessionId = C.AUDIO_SESSION_ID_UNSET
            resetMagnitudesLocked()
        }
    }

    fun release() {
        synchronized(lock) {
            isActiveRequested = false
            detachSession()
        }
    }

    private fun enableVisualizerLocked(sessionId: Int) {
        runCatching {
            val vis = Visualizer(sessionId)
            val captureRange = Visualizer.getCaptureSizeRange()
            val minCaptureSize = captureRange.firstOrNull()?.coerceAtLeast(128) ?: 128
            vis.captureSize = minCaptureSize

            val maxRate = Visualizer.getMaxCaptureRate().coerceAtLeast(1_000)
            val targetRate = (maxRate / 2).coerceIn(1, maxRate)

            vis.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (!isActiveRequested || fft == null || fft.size < 4) return
                        processFftFrame(fft)
                    }
                },
                targetRate,
                false,
                true,
            )

            vis.enabled = true
            visualizer = vis
        }.onFailure {
            releaseVisualizerLocked()
            resetMagnitudesLocked()
        }
    }

    private fun processFftFrame(fft: ByteArray) {
        val totalBins = (fft.size / 2).coerceAtLeast(2)
        val usableBins = (totalBins - 1).coerceAtLeast(1)
        val output = ArrayList<Float>(barCount)

        for (barIndex in 0 until barCount) {
            val startFrac = (barIndex.toFloat() / barCount.toFloat()).pow(1.35f)
            val endFrac = ((barIndex + 1).toFloat() / barCount.toFloat()).pow(1.35f)
            val startBin = (1 + (startFrac * (usableBins - 1)).toInt()).coerceIn(1, usableBins)
            val endBin = (1 + (endFrac * (usableBins - 1)).toInt()).coerceIn(startBin, usableBins)

            var peakMag = 0f
            for (k in startBin..endBin) {
                val real = fft[2 * k].toFloat()
                val imag = if (2 * k + 1 < fft.size) fft[2 * k + 1].toFloat() else 0f
                val mag = hypot(real, imag)
                if (mag > peakMag) peakMag = mag
            }

            val db = 20f * log10(peakMag + 1f)
            val normalized = ((db - NOISE_FLOOR_DB) / DYNAMIC_RANGE_DB).coerceIn(0f, 1f)
            val prev = smoothedBars[barIndex]
            val blended = if (normalized >= prev) {
                prev * 0.30f + normalized * 0.70f
            } else {
                prev * 0.55f + normalized * 0.45f
            }.coerceIn(0f, 1f)
            smoothedBars[barIndex] = blended
            output += blended
        }

        _magnitudes.value = output
    }

    private fun releaseVisualizerLocked() {
        val vis = visualizer ?: return
        runCatching { vis.enabled = false }
        runCatching { vis.setDataCaptureListener(null, 0, false, false) }
        runCatching { vis.release() }
        visualizer = null
    }

    private fun resetMagnitudesLocked() {
        smoothedBars.fill(0f)
        _magnitudes.value = zeroBars
    }

    companion object {
        private const val DEFAULT_BAR_COUNT = 22
        private const val NOISE_FLOOR_DB = 4f
        private const val DYNAMIC_RANGE_DB = 34f
    }
}
