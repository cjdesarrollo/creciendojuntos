package com.prestamos.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.export.ExcelCsvExporter
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.Payment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class BackupUiState(
    val isExporting: Boolean = false,
    val exportedFileUri: Uri? = null,
    val exportedFileName: String? = null,
    val message: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class BackupViewModel(
    private val context: Context,
    private val repository: LoanRepository,
    private val excelCsvExporter: ExcelCsvExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportExcelReport() {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Generando reporte Excel (.xlsx)...")
            try {
                val loans: List<Loan> = repository.getAllLoans().first()
                val clients: List<Client> = repository.getAllClients().first()
                val clientsMap = clients.associateBy { it.id }

                val file = excelCsvExporter.exportLoansToExcel(loans, clientsMap)
                if (file != null) {
                    val uri = FileProvider.getUriForFile(context, "com.prestamos.app.fileprovider", file)
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        exportedFileUri = uri,
                        exportedFileName = file.name,
                        message = "Reporte Excel (.xlsx) generado exitosamente"
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "No se pudo generar el archivo Excel")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al exportar a Excel")
            }
        }
    }

    fun exportCsvPaymentsReport() {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Generando reporte de cobros CSV...")
            try {
                val payments: List<Payment> = repository.getAllPayments().first()
                val loans: List<Loan> = repository.getAllLoans().first()
                val clients: List<Client> = repository.getAllClients().first()

                val loansMap = loans.associateBy { it.id }
                val clientsMap = clients.associateBy { it.id }

                val file = excelCsvExporter.exportPaymentsToCsv(payments, loansMap, clientsMap)
                if (file != null) {
                    val uri = FileProvider.getUriForFile(context, "com.prestamos.app.fileprovider", file)
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        exportedFileUri = uri,
                        exportedFileName = file.name,
                        message = "Reporte CSV de cobros generado exitosamente"
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "No se pudo generar el archivo CSV")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al exportar a CSV")
            }
        }
    }

    fun createBackupArchive() {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Creando archivo de respaldo cifrado (.backup)...")
            try {
                val backupFile = File(context.cacheDir, "PrestamoFacil_Backup_${System.currentTimeMillis()}.backup")
                val success = excelCsvExporter.createFullBackup(backupFile)
                if (success) {
                    val uri = FileProvider.getUriForFile(context, "com.prestamos.app.fileprovider", backupFile)
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        exportedFileUri = uri,
                        exportedFileName = backupFile.name,
                        message = "Respaldo (.backup) creado y listo para compartir o guardar"
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "Error al empaquetar el archivo de respaldo")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al crear respaldo")
            }
        }
    }

    fun restoreBackupFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Restaurando base de datos y archivos...")
            try {
                val tempBackupFile = File(context.cacheDir, "temp_restore.backup")
                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                FileOutputStream(tempBackupFile).use { out ->
                    inputStream?.copyTo(out)
                }

                val success = excelCsvExporter.restoreFullBackup(tempBackupFile)
                if (success) {
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        message = "¡Respaldo restaurado exitosamente! Reinicia la app para ver todos tus datos."
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "Error al restaurar el respaldo. Verifica la integridad del archivo.")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al procesar archivo de respaldo")
            }
        }
    }

    fun exportDb3Backup() {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Generando archivo de base de datos (.db3)...")
            try {
                val db3File = File(context.cacheDir, "PrestamoFacil_Backup_${System.currentTimeMillis()}.db3")
                val success = excelCsvExporter.exportDb3File(db3File)
                if (success) {
                    val uri = FileProvider.getUriForFile(context, "com.prestamos.app.fileprovider", db3File)
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        exportedFileUri = uri,
                        exportedFileName = db3File.name,
                        message = "Base de datos (.db3) generada exitosamente lista para compartir"
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "Error al generar el archivo .db3")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al exportar base de datos .db3")
            }
        }
    }

    fun restoreDb3FromUri(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isExporting = true, message = "Restaurando base de datos desde archivo .db3...")
            try {
                val tempDb3File = File(context.cacheDir, "temp_restore.db3")
                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                FileOutputStream(tempDb3File).use { out ->
                    inputStream?.copyTo(out)
                }

                val success = excelCsvExporter.restoreDb3File(tempDb3File)
                if (success) {
                    _uiState.value = BackupUiState(
                        isSuccess = true,
                        message = "¡Base de datos (.db3) restaurada con éxito! Reinicia la aplicación para refrescar los datos."
                    )
                } else {
                    _uiState.value = BackupUiState(errorMessage = "Error al restaurar el archivo .db3. Verifica que sea un archivo de base de datos válido.")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = e.localizedMessage ?: "Error al restaurar base de datos .db3")
            }
        }
    }
}
