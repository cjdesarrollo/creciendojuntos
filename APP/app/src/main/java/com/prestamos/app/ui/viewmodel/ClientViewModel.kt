package com.prestamos.app.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.ClientWithCompliance
import com.prestamos.app.domain.model.Loan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class ClientFormState(
    val editingClientId: Long? = null,
    val fullName: String = "",
    val dniOrId: String = "",
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    val reference1Name: String = "",
    val reference1Phone: String = "",
    val reference1Relation: String = "Familiar",
    val reference2Name: String = "",
    val reference2Phone: String = "",
    val reference2Relation: String = "Personal / Amigo",
    val estimatedInterestPercent: String = "",
    val idPhotoUri: String? = null,
    val docPhotoUri: String? = null,
    val signatureUri: String? = null,
    val isUncollectible: Boolean = false,
    val isUnderRecovery: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ClientViewModel(
    private val repository: LoanRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    val clients = repository.getAllClients()

    private val _selectedCompanyId = MutableStateFlow(0L) // 0L = Todas
    val selectedCompanyId: StateFlow<Long> = _selectedCompanyId.asStateFlow()

    val clientsWithCompliance: StateFlow<List<ClientWithCompliance>> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getClientsWithCompliance(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ClientFormState())
    val formState: StateFlow<ClientFormState> = _formState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onCompanyFilterChanged(companyId: Long) {
        _selectedCompanyId.value = companyId
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun prepareEditClient(client: Client) {
        _formState.value = ClientFormState(
            editingClientId = client.id,
            fullName = client.fullName,
            dniOrId = client.dniOrId,
            phone = client.phone,
            address = client.address,
            email = client.email ?: "",
            notes = client.notes ?: "",
            reference1Name = client.reference1Name ?: "",
            reference1Phone = client.reference1Phone ?: "",
            reference1Relation = client.reference1Relation ?: "Familiar",
            reference2Name = client.reference2Name ?: "",
            reference2Phone = client.reference2Phone ?: "",
            reference2Relation = client.reference2Relation ?: "Personal / Amigo",
            estimatedInterestPercent = client.estimatedInterestPercent?.toPlainString() ?: "",
            idPhotoUri = client.idPhotoUri,
            docPhotoUri = client.docPhotoUri,
            signatureUri = client.signatureUri,
            isUncollectible = client.isUncollectible,
            isUnderRecovery = client.isUnderRecovery
        )
    }

    fun onFormChanged(
        fullName: String = _formState.value.fullName,
        dniOrId: String = _formState.value.dniOrId,
        phone: String = _formState.value.phone,
        address: String = _formState.value.address,
        email: String = _formState.value.email,
        notes: String = _formState.value.notes,
        reference1Name: String = _formState.value.reference1Name,
        reference1Phone: String = _formState.value.reference1Phone,
        reference1Relation: String = _formState.value.reference1Relation,
        reference2Name: String = _formState.value.reference2Name,
        reference2Phone: String = _formState.value.reference2Phone,
        reference2Relation: String = _formState.value.reference2Relation,
        estimatedInterestPercent: String = _formState.value.estimatedInterestPercent
    ) {
        _formState.value = _formState.value.copy(
            fullName = fullName,
            dniOrId = dniOrId,
            phone = phone,
            address = address,
            email = email,
            notes = notes,
            reference1Name = reference1Name,
            reference1Phone = reference1Phone,
            reference1Relation = reference1Relation,
            reference2Name = reference2Name,
            reference2Phone = reference2Phone,
            reference2Relation = reference2Relation,
            estimatedInterestPercent = estimatedInterestPercent
        )
    }

    fun onIdPhotoPicked(uri: Uri) {
        val path = fileStorageManager.saveImageToPrivateStorage(uri, "id_photo")
        _formState.value = _formState.value.copy(idPhotoUri = path)
    }

    fun onDocPhotoPicked(uri: Uri) {
        val path = fileStorageManager.saveImageToPrivateStorage(uri, "doc_photo")
        _formState.value = _formState.value.copy(docPhotoUri = path)
    }

    fun onSignatureCaptured(bitmap: Bitmap) {
        val tempClientId = System.currentTimeMillis()
        val path = fileStorageManager.saveSignatureBitmap(bitmap, tempClientId)
        _formState.value = _formState.value.copy(signatureUri = path)
    }

    fun saveClient() {
        val state = _formState.value
        if (state.fullName.isBlank()) {
            _formState.value = state.copy(errorMessage = "El nombre es obligatorio")
            return
        }
        if (state.dniOrId.isBlank()) {
            _formState.value = state.copy(errorMessage = "La cédula / DNI es obligatoria")
            return
        }

        val estimatedInterest = state.estimatedInterestPercent.toBigDecimalOrNull()

        viewModelScope.launch {
            try {
                val client = Client(
                    id = state.editingClientId ?: 0L,
                    fullName = state.fullName.trim(),
                    dniOrId = state.dniOrId.trim(),
                    phone = state.phone.trim(),
                    address = state.address.trim(),
                    email = state.email.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    reference1Name = state.reference1Name.ifBlank { null },
                    reference1Phone = state.reference1Phone.ifBlank { null },
                    reference1Relation = state.reference1Relation.ifBlank { null },
                    reference2Name = state.reference2Name.ifBlank { null },
                    reference2Phone = state.reference2Phone.ifBlank { null },
                    reference2Relation = state.reference2Relation.ifBlank { null },
                    estimatedInterestPercent = estimatedInterest,
                    idPhotoUri = state.idPhotoUri,
                    docPhotoUri = state.docPhotoUri,
                    signatureUri = state.signatureUri,
                    isUncollectible = state.isUncollectible,
                    isUnderRecovery = state.isUnderRecovery
                )

                if (state.editingClientId != null && state.editingClientId > 0) {
                    repository.updateClient(client)
                } else {
                    repository.saveClient(client)
                }
                _formState.value = ClientFormState(isSavedSuccess = true)
            } catch (e: Exception) {
                _formState.value = state.copy(errorMessage = e.localizedMessage ?: "Error al guardar cliente")
            }
        }
    }

    fun setClientUncollectible(clientId: Long, isUncollectible: Boolean) {
        viewModelScope.launch {
            repository.updateClientUncollectibleStatus(clientId, isUncollectible)
        }
    }

    fun setClientRecovery(clientId: Long, isUnderRecovery: Boolean) {
        viewModelScope.launch {
            repository.updateClientRecoveryStatus(clientId, isUnderRecovery)
        }
    }

    fun getLoansForClient(clientId: Long): Flow<List<Loan>> {
        return repository.getLoansForClient(clientId)
    }

    fun resetForm() {
        _formState.value = ClientFormState()
    }
}
