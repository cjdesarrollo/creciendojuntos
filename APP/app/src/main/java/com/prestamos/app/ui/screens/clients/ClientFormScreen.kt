package com.prestamos.app.ui.screens.clients

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prestamos.app.ui.components.SignaturePad
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientFormScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()

    val idPhotoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onIdPhotoPicked(it) }
    }

    val docPhotoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onDocPhotoPicked(it) }
    }

    if (formState.isSavedSuccess) {
        viewModel.resetForm()
        onBack()
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
                    text = if (formState.editingClientId != null) "Editar Cliente" else "Nuevo Cliente",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
            }

            if (formState.errorMessage != null) {
                Text(text = formState.errorMessage!!, color = RubyRed, fontWeight = FontWeight.Bold)
            }

            // Datos Personales
            Text("Información Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            OutlinedTextField(
                value = formState.fullName,
                onValueChange = { viewModel.onFormChanged(fullName = it) },
                label = { Text("Nombre Completo *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.dniOrId,
                onValueChange = { viewModel.onFormChanged(dniOrId = it) },
                label = { Text("Cédula / DNI *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.phone,
                onValueChange = { viewModel.onFormChanged(phone = it) },
                label = { Text("Teléfono *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.address,
                onValueChange = { viewModel.onFormChanged(address = it) },
                label = { Text("Dirección de Residencia *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.email,
                onValueChange = { viewModel.onFormChanged(email = it) },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.estimatedInterestPercent,
                onValueChange = { viewModel.onFormChanged(estimatedInterestPercent = it) },
                label = { Text("Interés Estimado para este Cliente (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Referencia 1
            Text("Referencia Personal 1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            OutlinedTextField(
                value = formState.reference1Name,
                onValueChange = { viewModel.onFormChanged(reference1Name = it) },
                label = { Text("Nombre Referencia 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = formState.reference1Phone,
                onValueChange = { viewModel.onFormChanged(reference1Phone = it) },
                label = { Text("Teléfono Referencia 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = formState.reference1Relation,
                onValueChange = { viewModel.onFormChanged(reference1Relation = it) },
                label = { Text("Parentesco / Relación Referencia 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Referencia 2
            Text("Referencia Personal 2", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            OutlinedTextField(
                value = formState.reference2Name,
                onValueChange = { viewModel.onFormChanged(reference2Name = it) },
                label = { Text("Nombre Referencia 2") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = formState.reference2Phone,
                onValueChange = { viewModel.onFormChanged(reference2Phone = it) },
                label = { Text("Teléfono Referencia 2") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = formState.reference2Relation,
                onValueChange = { viewModel.onFormChanged(reference2Relation = it) },
                label = { Text("Parentesco / Relación Referencia 2") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Documentos Adjuntos y Fotos
            Text("Documentos y Fotos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { idPhotoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (formState.idPhotoUri != null) "Cédula Cargada" else "Foto Cédula")
                }

                OutlinedButton(
                    onClick = { docPhotoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (formState.docPhotoUri != null) "Pagaré Cargado" else "Foto Pagaré")
                }
            }

            // Firma Digital
            Text("Firma Digital del Cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SignaturePad(
                        onSignatureSaved = { bitmap ->
                            viewModel.onSignatureCaptured(bitmap)
                        }
                    )
                }
            }

            Button(
                onClick = { viewModel.saveClient() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar Cliente", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
