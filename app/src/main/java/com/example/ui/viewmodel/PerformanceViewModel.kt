package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RazorTuneApplication
import com.example.data.AudioQualityMode
import com.example.data.PerformanceSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PerformanceViewModel : ViewModel() {

    private val performanceStore = RazorTuneApplication.instance.performanceStore

    val settings: StateFlow<PerformanceSettings> = performanceStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PerformanceSettings(),
        )

    fun setAudioQuality(mode: AudioQualityMode) {
        viewModelScope.launch {
            performanceStore.setAudioQuality(mode)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            performanceStore.setSkipSilence(enabled)
        }
    }

    fun setPreloadNextTrack(enabled: Boolean) {
        viewModelScope.launch {
            performanceStore.setPreloadNextTrack(enabled)
        }
    }

    fun setReduceVisualEffects(enabled: Boolean) {
        viewModelScope.launch {
            performanceStore.setReduceVisualEffects(enabled)
        }
    }

    fun setAutoReduceOnLowBattery(enabled: Boolean) {
        viewModelScope.launch {
            performanceStore.setAutoReduceOnLowBattery(enabled)
        }
    }
}
