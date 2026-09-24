package com.prestamos.app.ui.screens.loans

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prestamos.app.data.export.AmortizationPdfGenerator
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.model.*
import com.prestamos.app.ui.components.AmortizationPreviewDialog
import com.prestamos.app.ui.components.PaymentFrequencyDropdownMenu
import com.prestamos.app.ui.theme.*
import com.prestamos.app.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLoanScreen(
    viewModel: LoanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.createFormState.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val capitalOrigins by viewModel.capitalOrigins.collectAsState()

    var showClientDialog by remember { mutableStateOf(false) }
    var clientSearchQuery by remember { mutableStateOf("") }

    var showGuaranteeDialog by remember { mutableStateOf(false) }
    var showDisbursementModal by remember { mutableStateOf(false) }
    var showAmortizationPreviewDialog by remember { mutableStateOf(false) }

    val fileStorageManager = remember { FileStorageManager(context) }
    val pdfGenerator = remember { AmortizationPdfGenerator(context, fileStorageManager) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun shareAmortization(c: Client, l: Loan, insts: List<Installment>) {
        val pdfFile = pdfGenerator.generateAmortizationPdf(
            clientName = c.fullName,
            clientDni = c.dniOrId,
            loan = l,
            installments = insts
        )
        pdfFile?.let { f ->
            val uri = fileStorageManager.getShareableContentUri(f)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Tabla de Amortización - ${c.fullName}")
                putExtra(Intent.EXTRA_TEXT, "Adjunto tabla de amortización y plan de pagos para ${c.fullName}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Tabla de Amortización"))
        }
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
                    text = "Nuevo Préstamo",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
            }

            if (state.errorMessage != null) {
                Surface(
                    color = RubyRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.errorMessage!!,
                        color = RubyRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Seleccionar Empresa
            Text("Empresa Emisora *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                listOf(Company.CRECIENDO_JUNTOS, Company.FACILITO).forEach { comp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .clickable { viewModel.onCompanySelectedInForm(comp.id) }
                    ) {
                        RadioButton(
                            selected = state.companyId == comp.id,
                            onClick = { viewModel.onCompanySelectedInForm(comp.id) }
                        )
                        Text(comp.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 2. Seleccionar Cliente con FILTRO DE BÚSQUEDA
            val selectedClient = clients.firstOrNull { it.id == state.selectedClientId }
            OutlinedTextField(
                value = selectedClient?.let { "${it.fullName} • ${it.dniOrId}" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Cliente del Catálogo *") },
                placeholder = { Text("Toca para buscar y seleccionar cliente...") },
                trailingIcon = {
                    IconButton(onClick = {
                        clientSearchQuery = ""
                        showClientDialog = true
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar Cliente", tint = PrimaryBlue)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clientSearchQuery = ""
                        showClientDialog = true
                    },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                    disabledLabelColor = NavyPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedClient?.isUncollectible == true) {
                Surface(
                    color = RubyRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RubyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLIENTE BLOQUEADO: Este cliente está marcado como INCOBRABLE. No está permitido otorgarle préstamos.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = RubyRed
                        )
                    }
                }
            }

            // 3. Selector de Moneda
            Text("Moneda del Préstamo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                CurrencyType.values().forEach { curr ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .clickable { viewModel.onCurrencySelected(curr) }
                    ) {
                        RadioButton(
                            selected = state.currency == curr,
                            onClick = { viewModel.onCurrencySelected(curr) }
                        )
                        Text(curr.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 4. Parámetros del Crédito
            OutlinedTextField(
                value = state.principalInput,
                onValueChange = { viewModel.onFormInputChanged(principal = it) },
                label = { Text("Monto Principal (${state.currency.symbol}) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.interestInput,
                    onValueChange = { viewModel.onFormInputChanged(interest = it) },
                    label = { Text("Interés Mensual (%) *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.totalInstallmentsInput,
                    onValueChange = { viewModel.onFormInputChanged(installments = it) },
                    label = { Text("Total Cuotas *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 5. Frecuencia de Pago
            PaymentFrequencyDropdownMenu(
                selectedFrequency = state.paymentFrequency,
                onFrequencySelected = { viewModel.onFormInputChanged(frequency = it) },
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Botones de Modal: Programar Desembolso y Garantías
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDisbursementModal = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📅 Desembolso & Capital", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showGuaranteeDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Garantías (${state.guarantees.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Resumen de Desembolso Programado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fecha Desembolso Programada:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(dateFormatter.format(Date(state.scheduledDisbursementDate)), fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    val totalSources = state.capitalSources.values.fold(BigDecimal.ZERO) { acc, c -> acc.add(c) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Capital Asignado:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${state.currency.symbol} $totalSources", fontWeight = FontWeight.Bold, color = if (totalSources.toPlainString() == state.principalInput) EmeraldGreen else AccentYellowDark)
                    }
                }
            }

            // 7. Previsualización de Amortización
            state.schedulePreview?.let { schedule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Resumen Calculado", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${state.currency.displayName} • ${state.totalInstallmentsInput} Cuotas",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Interés:", color = Color.White.copy(alpha = 0.8f))
                            Text("${state.currency.symbol} ${schedule.totalInterest}", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Otros Gastos / Seguros:", color = Color.White.copy(alpha = 0.8f))
                            Text("${state.currency.symbol} ${schedule.totalExtraFee}", fontWeight = FontWeight.Bold, color = AccentYellow)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monto Total a Pagar:", color = Color.White.copy(alpha = 0.8f))
                            Text("${state.currency.symbol} ${schedule.totalAmount}", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Cuota por Período:", color = Color.White.copy(alpha = 0.8f))
                            Text("${state.currency.symbol} ${schedule.installmentAmount}", fontWeight = FontWeight.Bold, color = AccentYellow, style = MaterialTheme.typography.titleLarge)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón para ver tabla completa de amortización
                        OutlinedButton(
                            onClick = { showAmortizationPreviewDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Tabla de Amortización Completa", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showAmortizationPreviewDialog) {
                    val client = clients.firstOrNull { it.id == state.selectedClientId }
                    AmortizationPreviewDialog(
                        schedule = schedule,
                        currency = state.currency,
                        frequency = state.paymentFrequency,
                        clientName = client?.fullName,
                        onDismiss = { showAmortizationPreviewDialog = false }
                    )
                }
            }

            // BOTÓN CREAR Y COMPARTIR AUTOMÁTICO
            Button(
                enabled = (selectedClient?.isUncollectible != true) && (state.schedulePreview != null),
                onClick = {
                    viewModel.createLoan { loanId, client, loan, installments ->
                        if (client != null) {
                            shareAmortization(client, loan, installments)
                        }
                        viewModel.resetCreateForm()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear y Compartir Amortización", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    // DIÁLOGO BUSCADOR / FILTRO DE CLIENTES
    if (showClientDialog) {
        val filteredClients = remember(clientSearchQuery, clients) {
            if (clientSearchQuery.isBlank()) clients
            else clients.filter {
                it.fullName.contains(clientSearchQuery, ignoreCase = true) ||
                it.dniOrId.contains(clientSearchQuery, ignoreCase = true) ||
                it.phone.contains(clientSearchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            title = { Text("Buscar y Seleccionar Cliente", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clientSearchQuery,
                        onValueChange = { clientSearchQuery = it },
                        label = { Text("Filtrar por Nombre, Cédula o Teléfono") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                        trailingIcon = {
                            if (clientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { clientSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (filteredClients.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron clientes coincidentes.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredClients) { client ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onClientSelected(client.id)
                                            showClientDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (state.selectedClientId == client.id) PrimaryBlue.copy(alpha = 0.1f) else CardSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(client.fullName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                if (client.isUncollectible) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = RubyRed.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "INCOBRABLE",
                                                            color = RubyRed,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text("DNI: ${client.dniOrId} • Tel: ${client.phone}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        if (state.selectedClientId == client.id) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClientDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // DIÁLOGO FLOTANTE: PROGRAMAR DESEMBOLSO Y ASIGNAR CAPITAL
    if (showDisbursementModal) {
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = state.scheduledDisbursementDate } }
        var currentEpoch by remember { mutableStateOf(state.scheduledDisbursementDate) }
        val principalBD = state.principalInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

        var originDropdownExpanded by remember { mutableStateOf(false) }
        var selectedOriginToAdd by remember { mutableStateOf<CapitalOrigin?>(capitalOrigins.firstOrNull()) }
        var amountToAddInput by remember { mutableStateOf("") }

        val datePickerDialog = remember {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    currentEpoch = cal.timeInMillis
                    viewModel.onFormInputChanged(scheduledDate = currentEpoch)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        AlertDialog(
            onDismissRequest = { showDisbursementModal = false },
            title = {
                Text(
                    text = "Programar Desembolso y Capital",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Fecha de Desembolso (Libre):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(currentEpoch)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha Desembolso") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerDialog.show() },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextPrimary)
                        )

                        Button(
                            onClick = { datePickerDialog.show() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir Fecha")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "2. Orígenes de Capital (Desplegable):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capital Requerido: ${state.currency.symbol} $principalBD",
                        style = MaterialTheme.typography.bodySmall,
                        color = NavyPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Selector Desplegable de Origen
                    if (capitalOrigins.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = originDropdownExpanded,
                            onExpandedChange = { originDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedOriginToAdd?.name ?: "Seleccionar Origen...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Origen de Capital") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = originDropdownExpanded,
                                onDismissRequest = { originDropdownExpanded = false }
                            ) {
                                capitalOrigins.forEach { origin ->
                                    DropdownMenuItem(
                                        text = { Text("${origin.name} (Disp: C$ ${origin.currentBalance})") },
                                        onClick = {
                                            selectedOriginToAdd = origin
                                            originDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountToAddInput,
                                onValueChange = { amountToAddInput = it },
                                label = { Text("Monto a Aportar") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    val amountBD = amountToAddInput.toBigDecimalOrNull()
                                    val origin = selectedOriginToAdd
                                    if (origin != null && amountBD != null && amountBD > BigDecimal.ZERO) {
                                        viewModel.updateCapitalSource(origin.id, amountBD)
                                        amountToAddInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("Asignar")
                            }
                        }
                    }

                    // Lista de Orígenes Asignados
                    Text("Orígenes Asignados:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    if (state.capitalSources.isEmpty()) {
                        Text("No se ha asignado capital de orígenes específicos.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        state.capitalSources.forEach { (originId, amount) ->
                            val originName = capitalOrigins.find { it.id == originId }?.name ?: "Origen #$originId"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightSurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$originName: ${state.currency.symbol} $amount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { viewModel.updateCapitalSource(originId, BigDecimal.ZERO) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = RubyRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDisbursementModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // DIÁLOGO REGISTRO DE GARANTÍAS CON FOTO
    if (showGuaranteeDialog) {
        var type by remember { mutableStateOf("VEHICULO") }
        var estimatedValue by remember { mutableStateOf("") }
        var stateDesc by remember { mutableStateOf("BUEN_ESTADO") }
        var description by remember { mutableStateOf("") }
        var mainPhotoUri by remember { mutableStateOf<String?>(null) }
        var circulationPhotoUri by remember { mutableStateOf<String?>(null) }

        val mainPhotoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            mainPhotoUri = uri?.toString()
        }

        val circPhotoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            circulationPhotoUri = uri?.toString()
        }

        val guaranteeTypes = listOf(
            "VEHICULO" to "Vehículo",
            "MUEBLE" to "Muebles",
            "EQUIPO" to "Equipo Tecnológico",
            "INMUEBLE" to "Bien Inmueble"
        )

        val statesList = listOf(
            "NUEVO" to "Nuevo",
            "USADO" to "Usado",
            "BUEN_ESTADO" to "En Buen Estado",
            "REGULAR" to "Regular"
        )

        AlertDialog(
            onDismissRequest = { showGuaranteeDialog = false },
            title = { Text("Registrar Garantía del Préstamo", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tipo de Garantía *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        guaranteeTypes.forEach { (k, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { type = k }
                            ) {
                                RadioButton(selected = type == k, onClick = { type = k })
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = estimatedValue,
                        onValueChange = { estimatedValue = it },
                        label = { Text("Valor Estimado (${state.currency.symbol})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Estado del Bien *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        statesList.forEach { (k, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { stateDesc = k }
                            ) {
                                RadioButton(selected = stateDesc == k, onClick = { stateDesc = k })
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción (Marca, Modelo, Serie, Placa)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { mainPhotoLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (mainPhotoUri != null) "Foto Cargada ✓" else "Foto Bien", style = MaterialTheme.typography.labelSmall)
                        }

                        if (type == "VEHICULO") {
                            OutlinedButton(
                                onClick = { circPhotoLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (circulationPhotoUri != null) "Circulación ✓" else "Circulación", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valBD = estimatedValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        viewModel.addGuarantee(
                            Guarantee(
                                type = type,
                                estimatedValue = valBD,
                                state = stateDesc,
                                description = description,
                                mainPhotoUri = mainPhotoUri,
                                circulationPhotoUri = circulationPhotoUri
                            )
                        )
                        showGuaranteeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Agregar Garantía")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuaranteeDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
