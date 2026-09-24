package com.prestamos.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.importer.ExcelImporterService
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.model.DashboardSummary
import com.prestamos.app.domain.model.Loan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: LoanRepository,
    private val excelImporterService: ExcelImporterService
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage = _importMessage.asStateFlow()

    private val _selectedCompanyId = MutableStateFlow(0L) // 0L = Todas
    val selectedCompanyId = _selectedCompanyId.asStateFlow()

    fun setCompanyFilter(companyId: Long) {
        _selectedCompanyId.value = companyId
    }

    val summary: StateFlow<DashboardSummary> = _selectedCompanyId
        .flatMapLatest { compId -> repository.getDashboardSummary(compId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary()
        )

    val loans: StateFlow<List<Loan>> = _selectedCompanyId
        .flatMapLatest { compId -> repository.getAllLoans(compId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importFromExcel(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importMessage.value = "Importando datos..."
            val result = excelImporterService.importFromExcel(uri)
            _isImporting.value = false
            if (result.isSuccess) {
                _importMessage.value = "Datos importados correctamente"
            } else {
                _importMessage.value = "Error al importar: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }
}
