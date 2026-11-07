package com.espe.arlenguadesenas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espe.arlenguadesenas.domain.model.GestureRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _recognitionState = MutableStateFlow(GestureRecognitionResult("EN ESPERA", 0f, false))
    val recognitionState: StateFlow<GestureRecognitionResult> = _recognitionState

    fun updateRecognition(result: GestureRecognitionResult) {
        viewModelScope.launch {
            _recognitionState.value = result
        }
    }
}
