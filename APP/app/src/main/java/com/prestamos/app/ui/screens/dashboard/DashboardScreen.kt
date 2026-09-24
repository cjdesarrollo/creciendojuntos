package com.prestamos.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.LoanRequest
import com.prestamos.app.domain.model.RiskLevel
import com.prestamos.app.domain.model.UpcomingPayment
import com.prestamos.app.ui.theme.AccentYellow
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.SecondaryGreen
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMenuClick: () -> Unit,
    onNavigateToNewLoan: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToCashAudit: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit
) {
    val summary by viewModel.summary.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val selectedCompanyId by viewModel.selectedCompanyId.collectAsState()

    var showCompanyMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importMessage) {
        importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = "Dashboard",
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
                            DropdownMenu(
                                expanded = showCompanyMenu,
                                onDismissRequest = { showCompanyMenu = false }
                            ) {
                                Company.values().forEach { company ->
                                    DropdownMenuItem(
                                        text = { Text(company.displayName) },
                                        onClick = {
                                            viewModel.setCompanyFilter(company.id)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    item {
                        DashboardMetricCard(
                            title = "Total\nPréstamos",
                            value = "${summary.totalLoansCount}",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = SecondaryGreen
                        )
                    }
                    item {
                        DashboardMetricCard(
                            title = "Saldo\nPendiente",
                            value = "C$ ${summary.pendingBalanceNio}",
                            icon = Icons.Default.AttachMoney,
                            iconColor = AccentYellowDark
                        )
                    }
                    item {
                        DashboardMetricCard(
                            title = "Ingresos\nMensuales",
                            value = "C$ ${summary.monthlyIncomeNio}",
                            icon = Icons.Default.TrendingUp,
                            iconColor = SecondaryGreen
                        )
                    }
                    item {
                        DashboardMetricCard(
                            title = "Nuevas\nSolicitudes",
                            value = "${summary.newRequestsCount}",
                            icon = Icons.Default.Work,
                            iconColor = RubyRed
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Resumen de Préstamos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(contentAlignment = Alignment.Center) {
                                DonutChartCanvas(
                                    onTime = summary.onTimePercent,
                                    delayed = summary.delayedPercent,
                                    highRisk = summary.highRiskPercent,
                                    cancelled = summary.cancelledPercent,
                                    modifier = Modifier.size(130.dp)
                                )
                                Text(
                                    text = "${summary.onTimePercent}%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChartLegendRow("Al Día", "${summary.onTimePercent}%", SecondaryGreen)
                                ChartLegendRow("Atrasados", "${summary.delayedPercent}%", AccentYellowDark)
                                ChartLegendRow("Riesgo Alto", "${summary.highRiskPercent}%", RubyRed)
                                ChartLegendRow("Cancelados", "${summary.cancelledPercent}%", PrimaryBlue)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Solicitudes Recientes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (summary.recentRequests.isEmpty()) {
                                Text("No hay solicitudes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            } else {
                                summary.recentRequests.forEach { req ->
                                    RecentRequestRow(request = req)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estado de Préstamos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            BarChartCanvas(modifier = Modifier.size(130.dp, 100.dp))

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("Bajo", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                Text("Medio", style = MaterialTheme.typography.labelSmall, color = AccentYellowDark, fontWeight = FontWeight.Bold)
                                Text("Alto", style = MaterialTheme.typography.labelSmall, color = RubyRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Próximos Pagos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cliente", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("Fecha", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (summary.upcomingPayments.isEmpty()) {
                                Text("Sin pagos pendientes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            } else {
                                summary.upcomingPayments.forEach { pay ->
                                    UpcomingPaymentRow(payment = pay)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun DonutChartCanvas(
    onTime: Int,
    delayed: Int,
    highRisk: Int,
    cancelled: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 22f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        val total = (onTime + delayed + highRisk + cancelled).coerceAtLeast(1).toFloat()
        val sweep1 = (onTime / total) * 360f
        val sweep2 = (delayed / total) * 360f
        val sweep3 = (highRisk / total) * 360f
        val sweep4 = (cancelled / total) * 360f

        var startAngle = -90f

        drawArc(
            color = SecondaryGreen,
            startAngle = startAngle,
            sweepAngle = sweep1,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += sweep1

        drawArc(
            color = AccentYellowDark,
            startAngle = startAngle,
            sweepAngle = sweep2,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += sweep2

        drawArc(
            color = RubyRed,
            startAngle = startAngle,
            sweepAngle = sweep3,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += sweep3

        drawArc(
            color = PrimaryBlue,
            startAngle = startAngle,
            sweepAngle = sweep4,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun BarChartCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = 24f

        drawRect(
            color = PrimaryBlue,
            topLeft = Offset(width * 0.2f - barWidth / 2, height * 0.5f),
            size = Size(barWidth, height * 0.5f)
        )

        drawRect(
            color = AccentYellowDark,
            topLeft = Offset(width * 0.5f - barWidth / 2, height * 0.3f),
            size = Size(barWidth, height * 0.7f)
        )

        drawRect(
            color = SecondaryGreen,
            topLeft = Offset(width * 0.8f - barWidth / 2, height * 0.1f),
            size = Size(barWidth, height * 0.9f)
        )
    }
}

@Composable
fun ChartLegendRow(label: String, percentStr: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Text(percentStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun RecentRequestRow(request: LoanRequest) {
    val (riskLabel, riskColor) = when (request.riskLevel) {
        RiskLevel.HIGH -> Pair("Alto", RubyRed)
        RiskLevel.MEDIUM -> Pair("Medio", AccentYellowDark)
        RiskLevel.LOW -> Pair("Bajo", SecondaryGreen)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val company = Company.values().find { it.id == request.companyId } ?: Company.ALL
        if (company.iconRes != null) {
            Image(
                painter = painterResource(id = company.iconRes),
                contentDescription = company.shortName,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = if (company.iconRes == null) 12.dp else 0.dp)) {
            Text(request.clientName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${request.currency.symbol} ${request.requestedAmount}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }

        Surface(
            color = riskColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = riskLabel,
                color = riskColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun UpcomingPaymentRow(payment: UpcomingPayment) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val company = Company.values().find { it.id == payment.companyId } ?: Company.ALL
        if (company.iconRes != null) {
            Image(
                painter = painterResource(id = company.iconRes),
                contentDescription = company.shortName,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
        }
        
        Text(
            text = payment.clientName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(start = if (company.iconRes == null) 4.dp else 0.dp)
        )
        Text(
            text = dateFormat.format(Date(payment.dueDate)),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
