package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Settings
import com.example.shoptracklite.data.ShopTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val wholesaleModeEnabled: Boolean = false,
    val currencyCode: String = "USD",
    val shopName: String = "",
    val crNumber: String = "",
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                repository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        wholesaleModeEnabled = settings?.wholesaleModeEnabled ?: false,
                        currencyCode = settings?.currencyCode ?: "USD",
                        shopName = settings?.shopName ?: "",
                        crNumber = settings?.crNumber ?: "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleWholesaleMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getSettingsSync()
                val settings = currentSettings.copy(wholesaleModeEnabled = enabled)
                repository.updateSettings(settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun updateCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getSettingsSync()
                val settings = currentSettings.copy(currencyCode = currencyCode)
                repository.updateSettings(settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun updateShopName(shopName: String) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getSettingsSync()
                val settings = currentSettings.copy(shopName = shopName)
                repository.updateSettings(settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun updateCrNumber(crNumber: String) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getSettingsSync()
                val settings = currentSettings.copy(crNumber = crNumber)
                repository.updateSettings(settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

