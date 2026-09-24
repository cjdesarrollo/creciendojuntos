package com.prestamos.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.local.security.BiometricAuthenticator
import com.prestamos.app.data.local.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isPinSetup: Boolean = false,
    val pinInput: String = "",
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

class AuthViewModel(
    private val securityManager: SecurityManager,
    private val biometricAuthenticator: BiometricAuthenticator? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkPinStatus()
    }

    fun checkPinStatus() {
        val isSetup = securityManager.isPinCreated()
        val isBioEnabled = securityManager.isBiometricEnabled()
        val isBioAvail = biometricAuthenticator?.isBiometricAvailable() ?: true
        _uiState.value = _uiState.value.copy(
            isPinSetup = isSetup,
            isAuthenticated = !isSetup,
            isBiometricAvailable = isBioAvail,
            isBiometricEnabled = isBioEnabled || isBioAvail
        )
    }

    fun lockApp() {
        _uiState.value = _uiState.value.copy(isAuthenticated = false, pinInput = "")
    }

    fun onPinDigitEntered(digit: String) {
        val currentPin = _uiState.value.pinInput
        if (currentPin.length < 6) {
            val newPin = currentPin + digit
            _uiState.value = _uiState.value.copy(pinInput = newPin, errorMessage = null)
            if (newPin.length >= 4) {
                verifyOrSavePin(newPin)
            }
        }
    }

    fun onPinDelete() {
        val currentPin = _uiState.value.pinInput
        if (currentPin.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                pinInput = currentPin.dropLast(1),
                errorMessage = null
            )
        }
    }

    fun verifyOrSavePin(pin: String) {
        viewModelScope.launch {
            if (!_uiState.value.isPinSetup) {
                securityManager.savePin(pin)
                _uiState.value = _uiState.value.copy(
                    isPinSetup = true,
                    isAuthenticated = true,
                    pinInput = ""
                )
            } else {
                val isValid = securityManager.verifyPin(pin)
                if (isValid) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        errorMessage = null,
                        pinInput = ""
                    )
                } else {
                    if (pin.length >= 6 || pin.length == 4) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "PIN incorrecto",
                            pinInput = ""
                        )
                    }
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true, errorMessage = null)
    }

    fun toggleBiometric(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }
}
