package com.prestamos.app.ui.screens.loans

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import com.prestamos.app.ui.components.AmortizationPreviewDialog
import java.math.RoundingMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prestamos.app.data.export.AmortizationPdfGenerator
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.calculator.AmortizationCalculator
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.ui.components.PaymentFrequencyDropdownMenu
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.prestamos.app.ui.theme.AccentBlue
import com.prestamos.app.ui.theme.AccentYellow
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.BorderSlate
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.EmeraldGreen
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmortizationSimulationScreen(
    viewModel: LoanViewModel,
    onNavigateToCreateLoan: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()

    var selectedCompanyId by remember { mutableStateOf(1L) }
    var selectedClientId by remember { mutableStateOf<Long?>(null) } // null = Cliente Genérico
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var currency by remember { mutableStateOf(CurrencyType.NIO) }
    var principalInput by remember { mutableStateOf("10000") }
    var interestInput by remember { mutableStateOf("10") }
    var totalInstallmentsInput by remember { mutableStateOf("5") }
    var frequency by remember { mutableStateOf(PaymentFrequency.MONTHLY) }

    val principal = principalInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val interestPercent = interestInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val totalInstallments = totalInstallmentsInput.toIntOrNull() ?: 0

    val fileStorageManager = remember { FileStorageManager(context) }
    val pdfGenerator = remember { AmortizationPdfGenerator(context, fileStorageManager) }

    val schedule = remember(principal, interestPercent, totalInstallments, frequency, currency) {
        if (principal > BigDecimal.ZERO && interestPercent >= BigDecimal.ZERO && totalInstallments > 0) {
            try {
                AmortizationCalculator.calculateSchedule(
                    principal = principal,
                    interestRatePercent = interestPercent,
                    totalInstallments = totalInstallments,
                    frequency = frequency,
                    currency = currency
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var showFullTableDialog by remember { mutableStateOf(false) }

    val sym = currency.symbol
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val selectedClient = clients.firstOrNull { it.id == selectedClientId }
    val clientDisplayName = selectedClient?.fullName ?: "Cliente Genérico"
    val clientDniName = selectedClient?.dniOrId ?: "000-000000-0000X"

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = NavyPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulador de Amortización",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                }
            }

            // Formulario de Simulación
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Parámetros de Simulación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)

                        // Selector de Empresa
                        Text("Empresa", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(Company.CRECIENDO_JUNTOS, Company.FACILITO).forEach { comp ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(end = 24.dp)
                                        .clickable { selectedCompanyId = comp.id }
                                ) {
                                    RadioButton(selected = selectedCompanyId == comp.id, onClick = { selectedCompanyId = comp.id })
                                    Text(comp.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Selector de Cliente (Existente vs Genérico)
                        ExposedDropdownMenuBox(
                            expanded = clientDropdownExpanded,
                            onExpandedChange = { clientDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = clientDisplayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cliente de la Simulación") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = clientDropdownExpanded,
                                onDismissRequest = { clientDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cliente Genérico") },
                                    onClick = {
                                        selectedClientId = null
                                        clientDropdownExpanded = false
                                    }
                                )
                                clients.forEach { client ->
                                    DropdownMenuItem(
                                        text = { Text("${client.fullName} (${client.dniOrId})") },
                                        onClick = {
                                            selectedClientId = client.id
                                            clientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Selector de Moneda
                        Row(modifier = Modifier.fillMaxWidth()) {
                            CurrencyType.values().forEach { curr ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(end = 24.dp)
                                        .clickable { currency = curr }
                                ) {
                                    RadioButton(selected = currency == curr, onClick = { currency = curr })
                                    Text(curr.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Capital Inicial
                        OutlinedTextField(
                            value = principalInput,
                            onValueChange = { principalInput = it },
                            label = { Text("Capital ($sym)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // % Interés Mensual
                        OutlinedTextField(
                            value = interestInput,
                            onValueChange = { interestInput = it },
                            label = { Text("% Interés Fijo Mensual") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Número de Cuotas
                        OutlinedTextField(
                            value = totalInstallmentsInput,
                            onValueChange = { totalInstallmentsInput = it },
                            label = { Text("Cantidad de Cuotas") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        PaymentFrequencyDropdownMenu(
                            selectedFrequency = frequency,
                            onFrequencySelected = { frequency = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Resumen de la Simulación Proyectada
            schedule?.let { s ->
                val totalInst = s.installments.size.coerceAtLeast(1)
                val countBD = BigDecimal(totalInst)
                val capitalPerInst = s.principal.divide(countBD, 2, RoundingMode.HALF_UP)
                val interestPerInst = s.totalInterest.divide(countBD, 2, RoundingMode.HALF_UP)
                val extraFeePerInst = s.extraFeePerInstallment

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Resumen Proyectado",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cliente: $clientDisplayName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${currency.displayName} • $totalInst Cuotas",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Interés Total", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("$sym ${s.totalInterest}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Otros Gastos / Seg.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("$sym ${s.totalExtraFee}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentYellow)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monto Total a Cobrar", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("$sym ${s.totalAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Cuota Regular Proyectada", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("$sym ${s.installmentAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccentYellow)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botón Vista Previa Tabla Completa
                        Button(
                            onClick = { showFullTableDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver Tabla", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Botón Exportar PDF de Amortización
                        Button(
                            onClick = {
                                val dummyLoan = Loan(
                                    clientId = selectedClientId ?: 0L,
                                    companyId = selectedCompanyId,
                                    currency = currency,
                                    principalAmount = s.principal,
                                    interestRatePercent = s.interestPercent,
                                    totalInterestAmount = s.totalInterest,
                                    totalAmountToPay = s.totalAmount,
                                    paymentFrequency = frequency,
                                    totalInstallments = s.installments.size,
                                    installmentAmount = s.installmentAmount,
                                    startDate = System.currentTimeMillis(),
                                    dueDate = s.installments.lastOrNull()?.dueDate ?: System.currentTimeMillis()
                                )

                                val pdfFile = pdfGenerator.generateAmortizationPdf(
                                    clientName = clientDisplayName,
                                    clientDni = clientDniName,
                                    loan = dummyLoan,
                                    installments = s.installments
                                )

                                pdfFile?.let { f ->
                                    val uri = fileStorageManager.getShareableContentUri(f)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF de Amortización"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Carta", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Botón Crear Préstamo con esta Simulación
                        Button(
                            onClick = onNavigateToCreateLoan,
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Crear Préstamo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tabla de Cuotas Proyectadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        TextButton(onClick = { showFullTableDialog = true }) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver Desglose Completo", color = PrimaryBlue, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                items(s.installments) { inst ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NavyPrimary.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "#${inst.installmentNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text("Vence: ${dateFormat.format(Date(inst.dueDate))}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(
                                    text = "$sym ${inst.expectedAmount}",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderSlate.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Capital", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("$sym $capitalPerInst", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Interés", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("$sym $interestPerInst", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Otros Gastos", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("$sym $extraFeePerInst", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = AccentYellowDark)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFullTableDialog && schedule != null) {
            AmortizationPreviewDialog(
                schedule = schedule,
                currency = currency,
                frequency = frequency,
                clientName = clientDisplayName,
                onDismiss = { showFullTableDialog = false }
            )
        }
    }
}
