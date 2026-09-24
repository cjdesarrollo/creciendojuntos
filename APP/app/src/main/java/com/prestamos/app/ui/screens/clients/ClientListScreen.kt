package com.prestamos.app.ui.screens.clients

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.ClientStatus
import com.prestamos.app.domain.model.ClientWithCompliance
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.LoanStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.prestamos.app.ui.theme.AccentBlue
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.EmeraldGreen
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientListScreen(
    viewModel: ClientViewModel,
    onMenuClick: () -> Unit = {},
    onNavigateToAddClient: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onNavigateToRefinance: (Long, Long?) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val clientsWithCompliance by viewModel.clientsWithCompliance.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedClientForDetail by remember { mutableStateOf<ClientWithCompliance?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.resetForm()
                    onNavigateToAddClient()
                },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Cliente")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding)
        ) {
            // Encabezado TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú Lateral", tint = NavyPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Directorio de Clientes",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                    Text(
                        text = "Catálogo compartido de clientes y estado crediticio",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Buscador de Clientes
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Buscar cliente o DNI...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            val filteredList = clientsWithCompliance.filter {
                it.client.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.client.dniOrId.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { item ->
                    ClientTableRowCardItem(
                        item = item,
                        onSelectClient = { selectedClientForDetail = it },
                        onEditClient = { client ->
                            viewModel.prepareEditClient(client)
                            showEditDialog = true
                        }
                    )
                }
            }
        }

        // Diálogo de Edición de Cliente
        if (showEditDialog) {
            EditClientDialog(
                viewModel = viewModel,
                onDismiss = {
                    showEditDialog = false
                    viewModel.resetForm()
                },
                onSaved = {
                    showEditDialog = false
                    viewModel.resetForm()
                }
            )
        }

        // Diálogo de Detalle, Historial y Gestión de Cobranza (Incobrable / Recuperación)
        selectedClientForDetail?.let { selected ->
            val liveItem = clientsWithCompliance.find { it.client.id == selected.client.id } ?: selected
            ClientDetailHistoryDialog(
                item = liveItem,
                viewModel = viewModel,
                onDismiss = { selectedClientForDetail = null },
                onNavigateToLoanDetail = onNavigateToLoanDetail,
                onNavigateToRefinance = onNavigateToRefinance
            )
        }
    }
}

@Composable
fun ClientTableRowCardItem(
    item: ClientWithCompliance,
    onSelectClient: (ClientWithCompliance) -> Unit,
    onEditClient: (Client) -> Unit
) {
    val client = item.client

    val (statusLabel, statusBg, statusText) = when (item.status) {
        ClientStatus.ACTIVE -> Triple("Activo", EmeraldGreen.copy(alpha = 0.15f), EmeraldGreen)
        ClientStatus.OVERDUE -> Triple("En Mora", RubyRed.copy(alpha = 0.15f), RubyRed)
        ClientStatus.INACTIVE -> Triple("Inactivo", Color.LightGray.copy(alpha = 0.3f), Color.DarkGray)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectClient(item) },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila 1: Avatar, Nombre, Cédula, Badge Estado y Botón Editar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = client.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary
                        )
                        Text(
                            text = "DNI: ${client.dniOrId} • Tel: ${client.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (client.isUncollectible) {
                        Surface(
                            color = RubyRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "INCOBRABLE",
                                color = RubyRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (client.isUnderRecovery) {
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "RECUPERACIÓN",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Badge Estado (Activo, Inactivo, En mora)
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Botón Editar Cliente
                    IconButton(onClick = { onEditClient(client) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Cliente", tint = AccentBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de Porcentaje de Avance del Préstamo por Cliente
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Avance de Préstamos: ${item.loanProgressPercent}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NavyPrimary
                    )
                    Text(
                        text = "Préstamos Activos: ${item.activeLoansCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (item.loanProgressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (item.status == ClientStatus.OVERDUE) RubyRed else EmeraldGreen,
                    trackColor = LightSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Información Adicional (Interés Estimado & Contactos de Referencia)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Interés Estimado", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = if (client.estimatedInterestPercent != null) "${client.estimatedInterestPercent}% mensual" else "No asignado",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("Ref. 1", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = client.reference1Name ?: "Sin registro",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("Ref. 2", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = client.reference2Name ?: "Sin registro",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun EditClientDialog(
    viewModel: ClientViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Editar Registro de Cliente", fontWeight = FontWeight.Bold, color = NavyPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.fullName,
                    onValueChange = { viewModel.onFormChanged(fullName = it) },
                    label = { Text("Nombre Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.dniOrId,
                    onValueChange = { viewModel.onFormChanged(dniOrId = it) },
                    label = { Text("Cédula / DNI *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.onFormChanged(phone = it) },
                    label = { Text("Teléfono *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.address,
                    onValueChange = { viewModel.onFormChanged(address = it) },
                    label = { Text("Dirección *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.estimatedInterestPercent,
                    onValueChange = { viewModel.onFormChanged(estimatedInterestPercent = it) },
                    label = { Text("Interés Estimado (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Referencia Extra 1", fontWeight = FontWeight.Bold, color = NavyPrimary, modifier = Modifier.padding(top = 4.dp))
                OutlinedTextField(
                    value = formState.reference1Name,
                    onValueChange = { viewModel.onFormChanged(reference1Name = it) },
                    label = { Text("Nombre Referencia 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.reference1Phone,
                    onValueChange = { viewModel.onFormChanged(reference1Phone = it) },
                    label = { Text("Teléfono Referencia 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Referencia Extra 2", fontWeight = FontWeight.Bold, color = NavyPrimary, modifier = Modifier.padding(top = 4.dp))
                OutlinedTextField(
                    value = formState.reference2Name,
                    onValueChange = { viewModel.onFormChanged(reference2Name = it) },
                    label = { Text("Nombre Referencia 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.reference2Phone,
                    onValueChange = { viewModel.onFormChanged(reference2Phone = it) },
                    label = { Text("Teléfono Referencia 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                formState.errorMessage?.let { err ->
                    Text(err, color = RubyRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveClient()
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ClientDetailHistoryDialog(
    item: ClientWithCompliance,
    viewModel: ClientViewModel,
    onDismiss: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToRefinance: (Long, Long?) -> Unit
) {
    val client = item.client
    val clientLoans by viewModel.getLoansForClient(client.id).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val activeLoans = remember(clientLoans) {
        clientLoans.filter { it.status == LoanStatus.ACTIVE || it.status == LoanStatus.OVERDUE }
    }
    val finishedLoans = remember(clientLoans) {
        clientLoans.filter { it.status == LoanStatus.PAID || it.status == LoanStatus.CANCELLED }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "DNI: ${client.dniOrId} • Tel: ${client.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estado del Cliente y Badges Especiales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (client.isUncollectible) {
                        Surface(
                            color = RubyRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "INCOBRABLE",
                                color = RubyRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (client.isUnderRecovery) {
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "EN RECUPERACIÓN",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        color = when (item.status) {
                            ClientStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.15f)
                            ClientStatus.OVERDUE -> RubyRed.copy(alpha = 0.15f)
                            ClientStatus.INACTIVE -> Color.LightGray.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.status.displayName,
                            color = when (item.status) {
                                ClientStatus.ACTIVE -> EmeraldGreen
                                ClientStatus.OVERDUE -> RubyRed
                                ClientStatus.INACTIVE -> Color.DarkGray
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Acciones de Gestión de Riesgo (Incobrable / Recuperación)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acciones de Cobranza y Riesgo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Marcar / Quitar Incobrable
                            OutlinedButton(
                                onClick = {
                                    viewModel.setClientUncollectible(client.id, !client.isUncollectible)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (client.isUncollectible) EmeraldGreen else RubyRed
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (client.isUncollectible) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (client.isUncollectible) "Desmarcar" else "Incobrable",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botón Mandar a Recuperación -> Refinanciar
                            Button(
                                onClick = {
                                    viewModel.setClientRecovery(client.id, true)
                                    val targetLoanId = activeLoans.firstOrNull()?.id
                                    onDismiss()
                                    onNavigateToRefinance(client.id, targetLoanId)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE65100)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "A Recuperación",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Sección 1: Créditos Activos
                Text(
                    text = "Créditos Activos (${activeLoans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                if (activeLoans.isEmpty()) {
                    Text(
                        text = "El cliente no tiene créditos activos actualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    activeLoans.forEach { loan ->
                        val loanProgress = if (loan.totalAmountToPay > BigDecimal.ZERO) {
                            loan.totalPaidAmount.multiply(BigDecimal(100))
                                .divide(loan.totalAmountToPay, 0, RoundingMode.HALF_UP)
                                .toInt().coerceIn(0, 100)
                        } else 0

                        val isLoanDue = System.currentTimeMillis() > loan.dueDate && loan.remainingBalance > BigDecimal.ZERO
                        val (loanStatusLabel, loanStatusBg, loanStatusColor) = when {
                            loan.status == LoanStatus.OVERDUE -> Triple("En Mora", RubyRed.copy(alpha = 0.15f), RubyRed)
                            isLoanDue -> Triple("Vencido", RubyRed.copy(alpha = 0.15f), RubyRed)
                            else -> Triple("Al Día", EmeraldGreen.copy(alpha = 0.15f), EmeraldGreen)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${loan.currency.symbol} ${loan.principalAmount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                    Surface(
                                        color = loanStatusBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = loanStatusLabel,
                                            color = loanStatusColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Saldo: ${loan.currency.symbol} ${loan.remainingBalance}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Vence: ${dateFormat.format(Date(loan.dueDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                // Barra de Progreso del Crédito
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Progreso de Pago",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "$loanProgress%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (loanProgress / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (loanStatusLabel == "Al Día") EmeraldGreen else RubyRed,
                                        trackColor = LightSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToLoanDetail(loan.id)
                                        }
                                    ) {
                                        Text("Ver Detalle", fontSize = 12.sp, color = NavyPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToRefinance(client.id, loan.id)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                                    ) {
                                        Text("Refinanciar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Sección 2: Historial de Créditos Finalizados
                Text(
                    text = "Historial de Créditos (${finishedLoans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                if (finishedLoans.isEmpty()) {
                    Text(
                        text = "Sin créditos anteriores completados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    finishedLoans.forEach { loan ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${loan.currency.symbol} ${loan.principalAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Iniciado: ${dateFormat.format(Date(loan.startDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Surface(
                                    color = if (loan.status == LoanStatus.PAID) EmeraldGreen.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = loan.status.displayName,
                                        color = if (loan.status == LoanStatus.PAID) EmeraldGreen else Color.DarkGray,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = NavyPrimary)
            }
        }
    )
}
