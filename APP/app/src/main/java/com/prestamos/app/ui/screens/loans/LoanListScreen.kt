package com.prestamos.app.ui.screens.loans

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prestamos.app.data.export.AmortizationPdfGenerator
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.RiskLevel
import com.prestamos.app.ui.theme.AccentYellow
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.SecondaryGreen
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.LoanViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanListScreen(
    viewModel: LoanViewModel,
    onMenuClick: () -> Unit = {},
    onNavigateToCreateLoan: () -> Unit,
    onNavigateToSimulate: () -> Unit = {},
    onNavigateToPayment: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val selectedCompanyId by viewModel.selectedCompanyId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<LoanStatus?>(null) }
    var showCompanyMenu by remember { mutableStateOf(false) }

    val fileStorageManager = remember { FileStorageManager(context) }
    val amortizationPdfGenerator = remember { AmortizationPdfGenerator(context, fileStorageManager) }

    val activeCount = loans.count { it.status == LoanStatus.ACTIVE }
    val delayedCount = loans.count { it.status == LoanStatus.OVERDUE }
    val cancelledCount = loans.count { it.status == LoanStatus.PAID || it.status == LoanStatus.CANCELLED }

    Scaffold(
        topBar = {
            Surface(
                color = PrimaryBlue,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onMenuClick) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú Lateral", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Préstamos",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCompanyMenu = true },
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentCompany = Company.values().find { it.id == selectedCompanyId } ?: Company.ALL
                                    Text(
                                        text = currentCompany.shortName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showCompanyMenu,
                                onDismissRequest = { showCompanyMenu = false }
                            ) {
                                Company.values().forEach { company ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(company.displayName) },
                                        onClick = {
                                            viewModel.onCompanyFilterChanged(company.id)
                                            showCompanyMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        BadgedBox(
                            badge = { Badge(containerColor = AccentYellow) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickFilterCard(
                        title = "Activos",
                        countStr = "$activeCount",
                        color = SecondaryGreen,
                        isSelected = filterStatus == LoanStatus.ACTIVE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            filterStatus = if (filterStatus == LoanStatus.ACTIVE) null else LoanStatus.ACTIVE
                        }
                    )
                    QuickFilterCard(
                        title = "Atrasados",
                        countStr = "$delayedCount",
                        color = AccentYellowDark,
                        isSelected = filterStatus == LoanStatus.OVERDUE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            filterStatus = if (filterStatus == LoanStatus.OVERDUE) null else LoanStatus.OVERDUE
                        }
                    )
                    QuickFilterCard(
                        title = "Cancelados",
                        countStr = "$cancelledCount",
                        color = PrimaryBlue,
                        isSelected = filterStatus == LoanStatus.PAID,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            filterStatus = if (filterStatus == LoanStatus.PAID) null else LoanStatus.PAID
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToCreateLoan,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nuevo Préstamo",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onNavigateToSimulate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulador",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialSummaryCard(
                        title = "Saldo Pendiente",
                        valueStr = "C$ ${summary.pendingBalanceNio}",
                        backgroundColor = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialSummaryCard(
                        title = "Próximos Pagos",
                        valueStr = "C$ ${summary.dueTodayAmountNio}",
                        backgroundColor = AccentYellowDark,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialSummaryCard(
                        title = "Altos Riesgos",
                        valueStr = "${summary.overdueLoansCount} Préstamos",
                        backgroundColor = RubyRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar cliente...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            val filteredLoans = loans.filter { loan ->
                val matchQuery = loan.clientName.contains(searchQuery, ignoreCase = true) || loan.id.toString().contains(searchQuery)
                val matchStatus = filterStatus == null || loan.status == filterStatus
                matchQuery && matchStatus
            }

            if (filteredLoans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontraron préstamos", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                items(filteredLoans) { loan ->
                    LoanCardItem(
                        loan = loan,
                        onMakePayment = { onNavigateToPayment(loan.id) },
                        onPrintPdf = {
                            val pdfFile = runBlocking {
                                val installments = viewModel.repository.getInstallmentsForLoan(loan.id).first()
                                amortizationPdfGenerator.generateAmortizationPdf(
                                    clientName = loan.clientName,
                                    clientDni = "ID #${loan.clientId}",
                                    loan = loan,
                                    installments = installments
                                )
                            }

                            pdfFile?.let { f ->
                                val uri = fileStorageManager.getShareableContentUri(f)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir Amortización PDF"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFilterCard(
    title: String,
    countStr: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) color else TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = countStr,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(
    title: String,
    valueStr: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(76.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valueStr,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = Color.White
            )
        }
    }
}

@Composable
fun LoanCardItem(
    loan: Loan,
    onMakePayment: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val company = Company.values().find { it.id == loan.companyId } ?: Company.ALL
    val sym = loan.currency.symbol

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onMakePayment() },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loan.clientName.ifBlank { "Cliente #${loan.clientId}" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
                Text(
                    text = "$sym ${loan.principalAmount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (company.iconRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = company.iconRes),
                        contentDescription = company.shortName,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = company.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Próximo Pago: ${dateFormat.format(Date(loan.dueDate))}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (riskLabel, riskColor, icon) = when {
                    loan.status == LoanStatus.OVERDUE || loan.riskLevel == RiskLevel.HIGH -> Triple("Alto Riesgo", RubyRed, Icons.Default.Error)
                    loan.riskLevel == RiskLevel.MEDIUM -> Triple("Atrasado", AccentYellowDark, Icons.Default.Warning)
                    else -> Triple("Bajo Riesgo", SecondaryGreen, Icons.Default.CheckCircle)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = riskColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = riskLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = riskColor
                    )
                }

                IconButton(onClick = onPrintPdf, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = PrimaryBlue)
                }
            }
        }
    }
}
