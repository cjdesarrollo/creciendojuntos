package com.prestamos.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.repository.ConfigRepository
import com.prestamos.app.domain.model.AppUser
import com.prestamos.app.domain.model.CompanyProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val companies: StateFlow<List<CompanyProfile>> = configRepository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<AppUser>> = configRepository.getAllActiveUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun saveCompany(company: CompanyProfile) {
        viewModelScope.launch {
            try {
                configRepository.saveCompany(company)
                _statusMessage.value = "Información de ${company.name} actualizada correctamente"
            } catch (e: Exception) {
                _statusMessage.value = "Error al actualizar empresa: ${e.localizedMessage}"
            }
        }
    }

    fun saveUser(user: AppUser, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                configRepository.saveUser(user)
                _statusMessage.value = if (user.id == 0L) "Usuario creado con éxito" else "Usuario actualizado"
                onComplete()
            } catch (e: Exception) {
                _statusMessage.value = "Error al guardar usuario: ${e.localizedMessage}"
            }
        }
    }

    fun deleteUser(user: AppUser) {
        viewModelScope.launch {
            try {
                configRepository.deleteUser(user)
                _statusMessage.value = "Usuario eliminado"
            } catch (e: Exception) {
                _statusMessage.value = "Error al eliminar usuario: ${e.localizedMessage}"
            }
        }
    }

    fun toggleUserBiometric(userId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                configRepository.setBiometricStatus(userId, enabled)
                _statusMessage.value = if (enabled) "Huella dactilar vinculada al usuario" else "Huella dactilar desvinculada"
            } catch (e: Exception) {
                _statusMessage.value = "Error al actualizar biometría: ${e.localizedMessage}"
            }
        }
    }

    fun updateUserPhoto(userId: Long, photoUri: String?) {
        viewModelScope.launch {
            try {
                configRepository.updateUserPhoto(userId, photoUri)
                _statusMessage.value = "Foto de perfil actualizada"
            } catch (e: Exception) {
                _statusMessage.value = "Error al actualizar foto: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
