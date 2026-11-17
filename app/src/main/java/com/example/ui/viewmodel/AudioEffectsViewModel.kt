package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RazorTuneApplication
import com.example.data.AudioEffectsSettings
import com.example.data.EqPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioEffectsViewModel : ViewModel() {

    private val audioEffectsStore = RazorTuneApplication.instance.audioEffectsStore

    val settings: StateFlow<AudioEffectsSettings> = audioEffectsStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AudioEffectsSettings(),
        )

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            audioEffectsStore.setEnabled(enabled)
        }
    }

    fun selectPreset(preset: EqPreset) {
        viewModelScope.launch {
            audioEffectsStore.selectPreset(preset)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        viewModelScope.launch {
            audioEffectsStore.setBandLevel(bandIndex, levelMb)
        }
    }

    fun setBassBoost(strength: Int) {
        viewModelScope.launch {
            audioEffectsStore.setBassBoost(strength)
        }
    }

    fun setVirtualizer(strength: Int) {
        viewModelScope.launch {
            audioEffectsStore.setVirtualizer(strength)
        }
    }

    fun setLoudnessGain(gainMb: Int) {
        viewModelScope.launch {
            audioEffectsStore.setLoudnessGain(gainMb)
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            audioEffectsStore.resetAll()
        }
    }
}
