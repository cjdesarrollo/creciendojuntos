package com.prestamos.app.ui.screens.payments

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prestamos.app.data.export.ExcelCsvExporter
import com.prestamos.app.data.export.PdfReceiptGenerator
import com.prestamos.app.data.export.ReceiptFormat
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.Payment
import com.prestamos.app.ui.theme.*
import com.prestamos.app.ui.viewmodel.LoanViewModel
import com.prestamos.app.ui.viewmodel.PaymentViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDashboardScreen(
    viewModel: LoanViewModel,
    paymentViewModel: PaymentViewModel,
    onMenuClick: () -> Unit,
    onNavigateToPayment: (Long) -> Unit
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsState()
    val allPayments by paymentViewModel.allPayments.collectAsState()
    val allClients by paymentViewModel.allClients.collectAsState()
    val allLoans by paymentViewModel.allLoans.collectAsState()

    val fileStorageManager = remember { FileStorageManager(context) }
    val excelExporter = remember { ExcelCsvExporter(context, fileStorageManager) }
    val pdfReceiptGenerator = remember { PdfReceiptGenerator(context, fileStorageManager) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCompanyFilter by remember { mutableStateOf(0L) } // 0 = Todas, 1 = CJ, 2 = Facilito

    // Filtros de Fecha para el Historial
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    val tabs = listOf("Pagos del Día", "Atrasados", "Historial de Pagos")
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val todayLoans = loans.filter { it.status == LoanStatus.ACTIVE }
    val overdueLoans = loans.filter { it.status == LoanStatus.OVERDUE }

    val clientsMap = remember(allClients) { allClients.associateBy { it.id } }
    val loansMap = remember(allLoans) { allLoans.associateBy { it.id } }

    // Filtrado de Historial de Pagos
    val filteredPayments = remember(allPayments, searchQuery, selectedCompanyFilter, startDateMillis, endDateMillis, clientsMap, loansMap) {
        allPayments.filter { p ->
            val loan = loansMap[p.loanId]
            val client = loan?.let { clientsMap[it.clientId] }
            val clientName = client?.fullName ?: ""
            val clientDni = client?.dniOrId ?: ""

            val matchesSearch = searchQuery.isBlank() ||
                clientName.contains(searchQuery, ignoreCase = true) ||
                clientDni.contains(searchQuery, ignoreCase = true) ||
                p.id.toString().contains(searchQuery) ||
                p.loanId.toString().contains(searchQuery)

            val matchesCompany = selectedCompanyFilter == 0L || p.companyId == selectedCompanyFilter

            val matchesDateStart = startDateMillis == null || p.paymentDate >= startDateMillis!!
            val matchesDateEnd = endDateMillis == null || p.paymentDate <= endDateMillis!!

            matchesSearch && matchesCompany && matchesDateStart && matchesDateEnd
        }
    }

    fun shareReceipt(payment: Payment) {
        val loan = loansMap[payment.loanId] ?: return
        val client = clientsMap[loan.clientId] ?: return

        val file = pdfReceiptGenerator.generateReceiptPdf(
            client = client,
            loan = loan,
            payment = payment,
            format = ReceiptFormat.HALF_LETTER_LANDSCAPE
        )
        file?.let { f ->
            val uri = fileStorageManager.getShareableContentUri(f)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Recibo de Pago #${payment.id} - ${client.fullName}")
                putExtra(Intent.EXTRA_TEXT, "Adjunto comprobante oficial de pago #${payment.id} por C$ ${payment.amountPaid}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Recibo de Pago"))
        }
    }

    fun exportToExcel() {
        val file = excelExporter.exportPaymentsToExcel(
            payments = filteredPayments,
            loansMap = loansMap,
            clientsMap = clientsMap
        )
        file?.let { f ->
            val uri = fileStorageManager.getShareableContentUri(f)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Pagos Aplicados")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte de pagos aplicados en formato Excel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Descargar / Compartir Reporte Excel"))
        }
    }

    val currentList = if (selectedTabIndex == 0) todayLoans else overdueLoans
    val filteredLoansList = currentList.filter {
        it.clientName.contains(searchQuery, ignoreCase = true) ||
        it.id.toString().contains(searchQuery)
    }

    Scaffold(
        topBar = {
            Surface(
                color = PrimaryBlue,
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Módulo de Pagos",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        if (selectedTabIndex == 2) {
                            Button(
                                onClick = { exportToExcel() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Descargar .xlsx", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        placeholder = { Text(if (selectedTabIndex == 2) "Filtrar por cliente, DNI o # recibo..." else "Buscar cliente para cobrar...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = AccentYellow,
                                height = 3.dp
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (selectedTabIndex == 2) {
            // PESTAÑA: HISTORIAL DE TODOS LOS PAGOS APLICADOS
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightSurface)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FILTROS AVANZADOS DE FECHA Y EMPRESA
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Filtro por Rango de Fecha", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = NavyPrimary)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Fecha Inicio
                                val startCal = remember { Calendar.getInstance() }
                                val startDatePicker = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        startCal.set(y, m, d, 0, 0, 0)
                                        startDateMillis = startCal.timeInMillis
                                    },
                                    startCal.get(Calendar.YEAR),
                                    startCal.get(Calendar.MONTH),
                                    startCal.get(Calendar.DAY_OF_MONTH)
                                )

                                OutlinedButton(
                                    onClick = { startDatePicker.show() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = startDateMillis?.let { "Desde: ${dateFormat.format(Date(it))}" } ?: "Fecha Inicio",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                // Fecha Fin
                                val endCal = remember { Calendar.getInstance() }
                                val endDatePicker = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        endCal.set(y, m, d, 23, 59, 59)
                                        endDateMillis = endCal.timeInMillis
                                    },
                                    endCal.get(Calendar.YEAR),
                                    endCal.get(Calendar.MONTH),
                                    endCal.get(Calendar.DAY_OF_MONTH)
                                )

                                OutlinedButton(
                                    onClick = { endDatePicker.show() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = endDateMillis?.let { "Hasta: ${dateFormat.format(Date(it))}" } ?: "Fecha Fin",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                if (startDateMillis != null || endDateMillis != null) {
                                    IconButton(onClick = {
                                        startDateMillis = null
                                        endDateMillis = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar Fechas", tint = RubyRed)
                                    }
                                }
                            }

                            // Botones rápidos de Rango
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        startDateMillis = cal.timeInMillis
                                        cal.set(Calendar.HOUR_OF_DAY, 23)
                                        cal.set(Calendar.MINUTE, 59)
                                        endDateMillis = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("Hoy", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        startDateMillis = cal.timeInMillis
                                        val endCal = Calendar.getInstance()
                                        endCal.set(Calendar.HOUR_OF_DAY, 23)
                                        endCal.set(Calendar.MINUTE, 59)
                                        endDateMillis = endCal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("Este Mes", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        startDateMillis = null
                                        endDateMillis = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextSecondary)
                                ) {
                                    Text("Todos", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Filtro por Empresa
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Empresa:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0L to "Todas", 1L to "Creciendo Juntos", 2L to "Facilito").forEach { (id, label) ->
                                        FilterChip(
                                            selected = selectedCompanyFilter == id,
                                            onClick = { selectedCompanyFilter = id },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Resumen de Pagos Filtrados
                item {
                    val totalSum = filteredPayments.map { it.amountPaid }.fold(BigDecimal.ZERO) { acc, c -> acc.add(c) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Recaudado en el Rango:", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                                Text("C$ $totalSum", color = AccentYellow, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cantidad:", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                                Text("${filteredPayments.size} pagos", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                if (filteredPayments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron pagos con los filtros seleccionados.", color = TextSecondary)
                        }
                    }
                } else {
                    items(filteredPayments) { payment ->
                        val loan = loansMap[payment.loanId]
                        val client = loan?.let { clientsMap[it.clientId] }
                        val clientName = client?.fullName ?: "Cliente #${payment.loanId}"
                        val isCj = payment.companyId == 1L

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = clientName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "Recibo #${payment.id} • Préstamo #${payment.loanId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "C$ ${payment.amountPaid}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                        Surface(
                                            color = if (isCj) NavyPrimary.copy(alpha = 0.12f) else PrimaryBlue.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (isCj) "Creciendo Juntos" else "Facilito",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCj) NavyPrimary else PrimaryBlue
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.4f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Fecha: ${dateTimeFormat.format(Date(payment.paymentDate))}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("Método: ${payment.paymentMethod.displayName} • Tipo: ${payment.paymentType}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }

                                    IconButton(
                                        onClick = { shareReceipt(payment) }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Compartir Recibo", tint = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PESTAÑAS 0 Y 1: PAGOS DEL DÍA / ATRASADOS
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightSurface)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredLoansList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay resultados en esta sección", color = TextSecondary)
                        }
                    }
                } else {
                    items(filteredLoansList) { loan ->
                        PaymentDashboardItem(loan, dateFormat) {
                            onNavigateToPayment(loan.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDashboardItem(loan: Loan, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = loan.clientName.ifBlank { "Cliente #${loan.clientId}" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Préstamo #${loan.id} • Vence: ${dateFormat.format(Date(loan.dueDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${loan.currency.symbol} ${loan.installmentAmount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (loan.status == LoanStatus.OVERDUE) RubyRed else EmeraldGreen
                )
                if (loan.status == LoanStatus.OVERDUE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RubyRed, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atrasado", style = MaterialTheme.typography.labelSmall, color = RubyRed)
                    }
                } else {
                    Text("Cuota", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}
