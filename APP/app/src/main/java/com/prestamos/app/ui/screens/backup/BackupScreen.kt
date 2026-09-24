package com.prestamos.app.ui.screens.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prestamos.app.ui.components.SectionHeader
import com.prestamos.app.ui.theme.AccentBlue
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.EmeraldGreen
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.BackupViewModel

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val restorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreBackupFromUri(it) }
    }

    val restoreDb3PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreDb3FromUri(it) }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = NavyPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Respaldos y Reportes Excel",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
            }

            if (state.message != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.12f))
                ) {
                    Text(text = state.message!!, modifier = Modifier.padding(16.dp), color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }

            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RubyRed.copy(alpha = 0.12f))
                ) {
                    Text(text = state.errorMessage!!, modifier = Modifier.padding(16.dp), color = RubyRed, fontWeight = FontWeight.Bold)
                }
            }

            // Exportar Reporte Excel (.xlsx)
            SectionHeader(title = "Reportes en Hoja de Cálculo", subtitle = "Exportación en formato Excel (.xlsx) y CSV")
            
            Button(
                onClick = { viewModel.exportExcelReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Préstamos a Excel (.xlsx)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
                onClick = { viewModel.exportCsvPaymentsReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Cobros a CSV", style = MaterialTheme.typography.titleMedium)
            }

            // Sistema de Respaldos Cifrados (.backup)
            SectionHeader(title = "Copia de Seguridad y Restauración", subtitle = "Respaldo local cifrado de toda la base de datos y archivos")

            Button(
                onClick = { viewModel.createBackupArchive() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Respaldo Cifrado (.backup)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
                onClick = { restorePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar Respaldo (.backup)", style = MaterialTheme.typography.titleMedium)
            }

            // Respaldo y Restauración en formato DB3 (SQLite)
            SectionHeader(title = "Base de Datos SQLite (.db3)", subtitle = "Generar archivo .db3 o restaurar directamente la base de datos")

            Button(
                onClick = { viewModel.exportDb3Backup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar Respaldo DB3 (.db3)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
                onClick = { restoreDb3PickerLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar desde Archivo DB3 (.db3)", style = MaterialTheme.typography.titleMedium)
            }

            // Compartir Archivo Generado
            state.exportedFileUri?.let { uri ->
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir Archivo"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir ${state.exportedFileName ?: "Archivo"}")
                }
            }
        }
    }
}
