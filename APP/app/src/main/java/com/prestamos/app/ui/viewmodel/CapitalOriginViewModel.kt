package com.prestamos.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.repository.CapitalOriginRepository
import com.prestamos.app.domain.model.CapitalOrigin
import com.prestamos.app.domain.model.CapitalTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CapitalOriginViewModel(
    private val repository: CapitalOriginRepository
) : ViewModel() {

    private val _origins = MutableStateFlow<List<CapitalOrigin>>(emptyList())
    val origins: StateFlow<List<CapitalOrigin>> = _origins.asStateFlow()

    private val _transactions = MutableStateFlow<List<CapitalTransaction>>(emptyList())
    val transactions: StateFlow<List<CapitalTransaction>> = _transactions.asStateFlow()

    private val _selectedOrigin = MutableStateFlow<CapitalOrigin?>(null)
    val selectedOrigin: StateFlow<CapitalOrigin?> = _selectedOrigin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadOrigins()
    }

    private fun loadOrigins() {
        viewModelScope.launch {
            repository.getAllOrigins().collectLatest {
                _origins.value = it
            }
        }
    }

    fun selectOrigin(id: Long) {
        viewModelScope.launch {
            repository.getOriginById(id).collectLatest {
                _selectedOrigin.value = it
            }
        }
        viewModelScope.launch {
            repository.getTransactionsForOrigin(id).collectLatest {
                _transactions.value = it
            }
        }
    }

    fun createOrigin(
        name: String,
        description: String? = null,
        companyId: Long = 0L,
        initialBalance: BigDecimal = BigDecimal.ZERO,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.createOrigin(name, description, companyId, initialBalance)
                _statusMessage.value = "Origen de capital '$name' creado con éxito"
                onComplete()
            } catch (e: Exception) {
                _statusMessage.value = "Error al crear origen: ${e.localizedMessage}"
            }
        }
    }

    fun updateOrigin(
        origin: CapitalOrigin,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateOrigin(origin)
                _statusMessage.value = "Origen '${origin.name}' actualizado"
                onComplete()
            } catch (e: Exception) {
                _statusMessage.value = "Error al actualizar origen: ${e.localizedMessage}"
            }
        }
    }

    fun increaseCapital(
        originId: Long,
        amount: BigDecimal,
        notes: String? = null,
        companyId: Long? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = repository.increaseCapital(originId, amount, notes, companyId)
                if (success) {
                    _statusMessage.value = "Capital aumentado exitosamente (+ $amount)"
                    onComplete()
                } else {
                    _statusMessage.value = "Monto inválido para aumento de capital"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al aumentar capital: ${e.localizedMessage}"
            }
        }
    }

    fun addTransaction(originId: Long, type: String, amount: BigDecimal, notes: String, companyId: Long? = null) {
        viewModelScope.launch {
            try {
                repository.addTransaction(originId, companyId, type, amount, notes)
                _statusMessage.value = "Transacción registrada con éxito"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun deleteOrigin(originId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = repository.deleteOrigin(originId)
                if (success) {
                    _statusMessage.value = "Origen de capital eliminado correctamente"
                    onComplete()
                } else {
                    _statusMessage.value = "No se pudo encontrar el origen para eliminar"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al eliminar origen: ${e.localizedMessage}"
            }
        }
    }
}
