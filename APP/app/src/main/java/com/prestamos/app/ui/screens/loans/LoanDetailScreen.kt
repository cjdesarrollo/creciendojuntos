package com.prestamos.app.ui.screens.loans

import android.app.DatePickerDialog
import android.content.Intent
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
import androidx.compose.ui.unit.sp
import com.prestamos.app.data.export.AmortizationPdfGenerator
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.calculator.AmortizationCalculator
import com.prestamos.app.domain.calculator.EarlySettlementQuote
import com.prestamos.app.domain.model.*
import com.prestamos.app.ui.components.PaymentFrequencyDropdownMenu
import com.prestamos.app.ui.theme.*
import com.prestamos.app.ui.viewmodel.PaymentViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: PaymentViewModel,
    onNavigateToPayment: (Long) -> Unit,
    onNavigateToRefinance: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fileStorageManager = remember { FileStorageManager(context) }
    val pdfGenerator = remember { AmortizationPdfGenerator(context, fileStorageManager) }

    var showCancelTotalDialog by remember { mutableStateOf(false) }
    var showEditLoanDialog by remember { mutableStateOf(false) }
    var showRefinanceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) {
        viewModel.loadLoanDetails(loanId)
    }

    val loan = state.selectedLoan
    val client = state.selectedClient
    val installments = state.installments

    fun shareStatementPdf(l: Loan, c: Client, insts: List<Installment>) {
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
                putExtra(Intent.EXTRA_SUBJECT, "Estado de Cuenta - ${c.fullName}")
                putExtra(Intent.EXTRA_TEXT, "Adjunto estado de cuenta y desglose de cuotas de ${c.fullName}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Estado de Cuenta"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Préstamo #${loanId}", color = NavyPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = NavyPrimary)
                    }
                },
                actions = {
                    if (loan != null && client != null) {
                        IconButton(onClick = { shareStatementPdf(loan, client, installments) }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir Estado de Cuenta", tint = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        }
    ) { innerPadding ->
        if (loan == null || client == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
            return@Scaffold
        }

        val canEditLoan = loan.totalPaidAmount.compareTo(BigDecimal.ZERO) == 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tarjeta de Resumen del Préstamo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("DNI: ${client.dniOrId} • Tel: ${client.phone}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            val compName = if (loan.companyId == 2L) "Facilito" else "Creciendo Juntos"
                            Text("Empresa: $compName", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = when (loan.status.name) {
                                "PAID" -> EmeraldGreen.copy(alpha = 0.15f)
                                "OVERDUE" -> RubyRed.copy(alpha = 0.15f)
                                else -> PrimaryBlue.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = loan.status.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = when (loan.status.name) {
                                    "PAID" -> EmeraldGreen
                                    "OVERDUE" -> RubyRed
                                    else -> PrimaryBlue
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Capital Original", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${loan.currency.symbol} ${loan.principalAmount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Cobrado", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${loan.currency.symbol} ${loan.totalPaidAmount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo Pendiente", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "${loan.currency.symbol} ${loan.remainingBalance}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (loan.remainingBalance > BigDecimal.ZERO) RubyRed else EmeraldGreen
                            )
                        }
                    }
                }
            }

            // Botón Principal de Estado de Cuenta PDF
            Button(
                onClick = { shareStatementPdf(loan, client, installments) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("📄 Generar y Compartir Estado de Cuenta (PDF)", fontWeight = FontWeight.Bold)
            }

            // Acciones Rápidas
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigateToPayment(loan.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    enabled = loan.remainingBalance > BigDecimal.ZERO
                ) {
                    Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cobrar Cuota")
                }

                Button(
                    onClick = { showRefinanceDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellowDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refinanciar")
                }
            }

            // Opciones Avanzadas: Cancelación Anticipada y Edición si no tiene pagos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loan.remainingBalance > BigDecimal.ZERO) {
                    OutlinedButton(
                        onClick = { showCancelTotalDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = RubyRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelación Anticipada", color = RubyRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (canEditLoan) {
                    OutlinedButton(
                        onClick = { showEditLoanDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar Préstamo", color = NavyPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Text("Cronograma de Cuotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(installments) { inst ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cuota #${inst.installmentNumber}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("Vencimiento: ${dateFormat.format(Date(inst.dueDate))}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${loan.currency.symbol} ${inst.expectedAmount}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (inst.remainingAmount > BigDecimal.ZERO) RubyRed else EmeraldGreen
                                )
                                Text(
                                    text = inst.status.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (inst.status.name) {
                                        "PAID" -> EmeraldGreen
                                        "OVERDUE" -> RubyRed
                                        else -> TextSecondary
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE CANCELACIÓN ANTICIPADA JUSTA
    if (showCancelTotalDialog && loan != null) {
        val earlyQuote: EarlySettlementQuote = remember(loan, installments) {
            AmortizationCalculator.calculateEarlySettlement(loan, installments)
        }

        AlertDialog(
            onDismissRequest = { showCancelTotalDialog = false },
            title = { Text("Cancelación Anticipada (Liquidación Justa)", fontWeight = FontWeight.Bold, color = RubyRed) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Se calcula el monto exacto cobrando únicamente el capital pendiente y los intereses causados a la fecha, exonerando los intereses futuros no devengados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Capital Pendiente:", style = MaterialTheme.typography.bodySmall)
                                Text("${loan.currency.symbol} ${earlyQuote.remainingPrincipal}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Intereses a la Fecha:", style = MaterialTheme.typography.bodySmall)
                                Text("${loan.currency.symbol} ${earlyQuote.accruedInterest}", fontWeight = FontWeight.Bold)
                            }
                            if (earlyQuote.accruedOtherFees > BigDecimal.ZERO) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Otros Gastos:", style = MaterialTheme.typography.bodySmall)
                                    Text("${loan.currency.symbol} ${earlyQuote.accruedOtherFees}", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (earlyQuote.interestDiscountSaved > BigDecimal.ZERO) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ahorro al Cliente:", style = MaterialTheme.typography.bodySmall, color = EmeraldGreen)
                                    Text("- ${loan.currency.symbol} ${earlyQuote.interestDiscountSaved}", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total a Liquidar:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RubyRed)
                                Text("${loan.currency.symbol} ${earlyQuote.totalToPay}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RubyRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordEarlySettlementPayment(loan.id, earlyQuote) {
                            showCancelTotalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRed)
                ) {
                    Text("Confirmar Pago (${loan.currency.symbol} ${earlyQuote.totalToPay})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelTotalDialog = false }) { Text("Volver") }
            }
        )
    }

    // DIÁLOGO EDITAR PRÉSTAMO SIN PAGOS
    if (showEditLoanDialog && loan != null) {
        var companyId by remember { mutableStateOf(loan.companyId) }
        var principal by remember { mutableStateOf(loan.principalAmount.toPlainString()) }
        var interest by remember { mutableStateOf(loan.interestRatePercent.toPlainString()) }
        var installmentsCount by remember { mutableStateOf(loan.totalInstallments.toString()) }
        var frequency by remember { mutableStateOf(loan.paymentFrequency) }
        var scheduledDate by remember { mutableStateOf(loan.scheduledDisbursementDate) }

        val datePicker = remember {
            val cal = Calendar.getInstance().apply { timeInMillis = scheduledDate }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val c = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    scheduledDate = c.timeInMillis
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        AlertDialog(
            onDismissRequest = { showEditLoanDialog = false },
            title = { Text("Editar Préstamo #${loan.id}", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Empresa Emisora", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(Company.CRECIENDO_JUNTOS, Company.FACILITO).forEach { comp ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable { companyId = comp.id }
                            ) {
                                RadioButton(selected = companyId == comp.id, onClick = { companyId = comp.id })
                                Text(comp.displayName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = principal,
                        onValueChange = { principal = it },
                        label = { Text("Capital Principal (${loan.currency.symbol})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = interest,
                            onValueChange = { interest = it },
                            label = { Text("Interés (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = installmentsCount,
                            onValueChange = { installmentsCount = it },
                            label = { Text("Cuotas") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Frecuencia de Pago", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PaymentFrequency.values().forEach { freq ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { frequency = freq }
                            ) {
                                RadioButton(selected = frequency == freq, onClick = { frequency = freq })
                                Text(freq.displayName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateFormat.format(Date(scheduledDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha Desembolso") },
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextPrimary)
                        )
                        Button(onClick = { datePicker.show() }, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pBD = principal.toBigDecimalOrNull() ?: loan.principalAmount
                        val iBD = interest.toBigDecimalOrNull() ?: loan.interestRatePercent
                        val cInt = installmentsCount.toIntOrNull() ?: loan.totalInstallments

                        viewModel.updateLoanDetails(
                            loanId = loan.id,
                            companyId = companyId,
                            principal = pBD,
                            interestPercent = iBD,
                            totalInstallments = cInt,
                            frequency = frequency,
                            startDateMillis = loan.startDate,
                            scheduledDate = scheduledDate,
                            capitalSources = emptyMap()
                        ) {
                            showEditLoanDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLoanDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO REFINANCIAR (MÉTODO REFINANCIAMIENTO Y MÉTODO PARALELO)
    if (showRefinanceDialog && loan != null && client != null) {
        RefinanceDialog(
            loan = loan,
            client = client,
            installments = installments,
            onDismiss = { showRefinanceDialog = false },
            onRefinanceConfirmed = { newPrincipal, interestPercent, totalInstallments, frequency, additionalDisbursement ->
                viewModel.refinanceLoan(
                    oldLoanId = loan.id,
                    companyId = loan.companyId,
                    clientId = loan.clientId,
                    currency = loan.currency,
                    newPrincipal = newPrincipal,
                    interestPercent = interestPercent,
                    totalInstallments = totalInstallments,
                    frequency = frequency,
                    additionalDisbursement = additionalDisbursement
                ) { newLoanId ->
                    showRefinanceDialog = false
                }
            },
            onParallelLoanRequested = {
                showRefinanceDialog = false
                onNavigateToRefinance(loan.clientId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefinanceDialog(
    loan: Loan,
    client: Client,
    installments: List<Installment>,
    onDismiss: () -> Unit,
    onRefinanceConfirmed: (newPrincipal: BigDecimal, interestPercent: BigDecimal, totalInstallments: Int, frequency: PaymentFrequency, additionalDisbursement: BigDecimal) -> Unit,
    onParallelLoanRequested: () -> Unit
) {
    var selectedMethodTab by remember { mutableStateOf(0) }
    val tabs = listOf("Refinanciamiento", "Crédito Paralelo")

    val earlyQuote = remember(loan, installments) {
        AmortizationCalculator.calculateEarlySettlement(loan, installments)
    }

    // Base a consolidar = capital pendiente + interés corriente
    val consolidatedSubtotal = earlyQuote.remainingPrincipal.add(earlyQuote.accruedInterest)

    var additionalDisbursementInput by remember { mutableStateOf("0") }
    var interestInput by remember { mutableStateOf(loan.interestRatePercent.toPlainString()) }
    var totalInstallmentsInput by remember { mutableStateOf("5") }
    var selectedFrequency by remember { mutableStateOf(loan.paymentFrequency) }

    val additionalDisbursement = additionalDisbursementInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val totalNewPrincipal = consolidatedSubtotal.add(additionalDisbursement)
    val interestPercent = interestInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val totalInstallments = totalInstallmentsInput.toIntOrNull() ?: 0

    val previewSchedule = remember(totalNewPrincipal, interestPercent, totalInstallments, selectedFrequency) {
        if (totalNewPrincipal > BigDecimal.ZERO && interestPercent >= BigDecimal.ZERO && totalInstallments > 0) {
            try {
                AmortizationCalculator.calculateSchedule(
                    principal = totalNewPrincipal,
                    interestRatePercent = interestPercent,
                    totalInstallments = totalInstallments,
                    frequency = selectedFrequency,
                    currency = loan.currency
                )
            } catch (e: Exception) {
                null
            }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Opciones de Refinanciamiento", fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text("Cliente: ${client.fullName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(selectedTabIndex = selectedMethodTab, containerColor = LightSurface) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedMethodTab == index,
                            onClick = { selectedMethodTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedMethodTab == 0) {
                    // MÉTODO 1: REFINANCIAMIENTO CONSOLIDADO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Deuda Actual a Consolidar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Capital Pendiente:", style = MaterialTheme.typography.bodySmall)
                                Text("${loan.currency.symbol} ${earlyQuote.remainingPrincipal}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Interés Corriente:", style = MaterialTheme.typography.bodySmall)
                                Text("${loan.currency.symbol} ${earlyQuote.accruedInterest}", fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal Deuda:", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text("${loan.currency.symbol} $consolidatedSubtotal", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = additionalDisbursementInput,
                        onValueChange = { additionalDisbursementInput = it },
                        label = { Text("Monto Adicional a Desembolsar (${loan.currency.symbol})") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nuevo Capital Total:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = EmeraldGreen)
                            Text("${loan.currency.symbol} $totalNewPrincipal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = EmeraldGreen)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = interestInput,
                            onValueChange = { interestInput = it },
                            label = { Text("Interés Mensual (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = totalInstallmentsInput,
                            onValueChange = { totalInstallmentsInput = it },
                            label = { Text("Total Cuotas") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    PaymentFrequencyDropdownMenu(
                        selectedFrequency = selectedFrequency,
                        onFrequencySelected = { selectedFrequency = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    previewSchedule?.let { sch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Nueva Cuota Regular:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                    Text("${loan.currency.symbol} ${sch.installmentAmount}", fontWeight = FontWeight.Bold, color = AccentYellow)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Monto Total Nuevo:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                    Text("${loan.currency.symbol} ${sch.totalAmount}", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // MÉTODO 2: CRÉDITO PARALELO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Crédito Paralelo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                            Text(
                                text = "El crédito actual #${loan.id} continuará activo con sus cuotas y fechas originales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Se abrirá la pantalla de creación de préstamos con el cliente '${client.fullName}' preseleccionado para emitir un crédito independiente en paralelo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMethodTab == 0) {
                Button(
                    onClick = {
                        if (totalNewPrincipal > BigDecimal.ZERO && totalInstallments > 0) {
                            onRefinanceConfirmed(
                                totalNewPrincipal,
                                interestPercent,
                                totalInstallments,
                                selectedFrequency,
                                additionalDisbursement
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellowDark),
                    enabled = totalNewPrincipal > BigDecimal.ZERO && totalInstallments > 0
                ) {
                    Text("Confirmar y Refinanciar", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onParallelLoanRequested,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Crear Crédito Paralelo", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
