package com.danieljm.bussin.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadWelcome() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            // placeholder load
            kotlinx.coroutines.delay(150)
            _uiState.value = _uiState.value.copy(isLoading = false, message = "Welcome to Bussin")
        }
    }
}

