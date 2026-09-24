package com.prestamos.app.ui.screens.cashaudit

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import com.prestamos.app.domain.model.CashMovement
import com.prestamos.app.domain.model.CashMovementType
import com.prestamos.app.domain.model.Company
import com.prestamos.app.ui.theme.AccentYellow
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.EmeraldGreen
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.CashAuditViewModel
import com.prestamos.app.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CashAuditScreen(
    viewModel: CashAuditViewModel,
    loanViewModel: LoanViewModel,
    onMenuClick: () -> Unit,
    onNavigateToLoanPayment: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val loans by loanViewModel.loans.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isIncomePickerOpen by remember { mutableStateOf(false) }
    var showCompanyMenu by remember { mutableStateOf(false) }
    val selectedCompanyId by viewModel.selectedCompanyId.collectAsState()

    val sym = state.selectedCurrency.symbol

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
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Caja",
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
            // 1. Tarjetas de Resumen de Métricas (Fila de 4 tarjetas)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashMetricCard("Ingresos\ndel Día", "$sym ${state.dailyIncomeTotal}", EmeraldGreen, Modifier.weight(1f))
                    CashMetricCard("Egresos\ndel Día", "$sym ${state.dailyExpenseTotal}", AccentYellowDark, Modifier.weight(1f))
                    CashMetricCard("Saldo\nActual", "$sym ${activeSession?.currentBalance ?: BigDecimal.ZERO}", PrimaryBlue, Modifier.weight(1f))
                    CashMetricCard("Diferencias", "$sym ${state.calculatedDifference}", RubyRed, Modifier.weight(1f))
                }
            }

            // 2. Tres Botones de Acción Principal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Verde: Registrar Ingreso -> Redirige a Cobro de Cuotas
                    Button(
                        onClick = { isIncomePickerOpen = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                            Text("Registrar ingreso", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Amarillo: Registrar Egreso -> Modal de Gasto con Concepto Obligatorio
                    Button(
                        onClick = { viewModel.openExpenseDialog() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellowDark)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                            Text("Registrar egreso", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Azul: Cierre de Caja
                    Button(
                        onClick = { viewModel.performAudit() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            Text("Cierre de caja", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Resumen General del Mes Actual
            item {
                val monthName = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date()).replaceFirstChar { it.uppercase() }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resumen General: $monthName",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${state.monthlyMovementCount} movs",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Ingresos Mes", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("$sym ${state.monthlyIncomeTotal}", fontWeight = FontWeight.Bold, color = EmeraldGreen, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Egresos Mes", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("$sym ${state.monthlyExpenseTotal}", fontWeight = FontWeight.Bold, color = AccentYellow, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Flujo Neto Mes", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text(
                                    text = "$sym ${state.monthlyNetBalance}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.monthlyNetBalance >= BigDecimal.ZERO) Color.White else RubyRed,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }

            // 4. Buscador de Movimientos
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar movimiento...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // 5. Historial de Movimientos Header
            item {
                Text(
                    text = "Historial de Movimientos por Día",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
            }

            // 6. Lista de Movimientos agrupados por días
            val filteredMovements = movements.filter { mov ->
                mov.concept.contains(searchQuery, ignoreCase = true) ||
                mov.category?.contains(searchQuery, ignoreCase = true) == true
            }

            if (filteredMovements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay movimientos registrados", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayDayFormat = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
                val todayKey = dayFormat.format(Date())
                val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yesterdayKey = dayFormat.format(calYesterday.time)

                val groupedMovements = filteredMovements
                    .sortedByDescending { it.date }
                    .groupBy { dayFormat.format(Date(it.date)) }

                groupedMovements.forEach { (dayKey, dayMovs) ->
                    val dayIncome = dayMovs.filter { it.type == CashMovementType.INCOME }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }
                    val dayExpense = dayMovs.filter { it.type == CashMovementType.EXPENSE }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }

                    val headerTitle = when (dayKey) {
                        todayKey -> "Hoy - ${displayDayFormat.format(Date(dayMovs.first().date))}"
                        yesterdayKey -> "Ayer - ${displayDayFormat.format(Date(dayMovs.first().date))}"
                        else -> displayDayFormat.format(Date(dayMovs.first().date)).replaceFirstChar { it.uppercase() }
                    }

                    item(key = "day_header_$dayKey") {
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = NavyPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dayIncome > BigDecimal.ZERO) {
                                        Text(
                                            text = "+ $sym $dayIncome",
                                            color = EmeraldGreen,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (dayExpense > BigDecimal.ZERO) {
                                        Text(
                                            text = "- $sym $dayExpense",
                                            color = RubyRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(dayMovs, key = { it.id }) { mov ->
                        CashMovementItemRow(movement = mov)
                    }
                }
            }
        }
    }

    // Modal de Registro de Egreso con Concepto Obligatorio
    if (state.isExpenseDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeExpenseDialog() },
            title = { Text("Registrar Egreso de Caja", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.errorMessage != null) {
                        Text(text = state.errorMessage!!, color = RubyRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = state.expenseAmountInput,
                        onValueChange = { viewModel.onExpenseAmountChanged(it) },
                        label = { Text("Monto del Egreso (C$) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = state.expenseConceptInput,
                        onValueChange = { viewModel.onExpenseConceptChanged(it) },
                        label = { Text("Concepto del Egreso * (Obligatorio)") },
                        placeholder = { Text("Ej. Compra de Insumos, Pago de Servicios") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.registerExpense() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellowDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar Egreso", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeExpenseDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Selector para "Registrar Ingreso" -> Redirigir a Cobro de Cuotas
    if (isIncomePickerOpen) {
        AlertDialog(
            onDismissRequest = { isIncomePickerOpen = false },
            title = { Text("Seleccionar Préstamo para Cobro", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    val activeLoans = loans.filter { it.remainingBalance > BigDecimal.ZERO }
                    if (activeLoans.isEmpty()) {
                        item { Text("No hay préstamos con saldo pendiente", color = TextSecondary) }
                    } else {
                        items(activeLoans) { loan ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isIncomePickerOpen = false
                                        onNavigateToLoanPayment(loan.id)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(loan.clientName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Saldo: C$ ${loan.remainingBalance}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text("Cobrar", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isIncomePickerOpen = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun CashMetricCard(
    title: String,
    valueStr: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.replace("\n", " "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueStr,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun CashMovementItemRow(movement: CashMovement) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(movement.date))

    val (badgeText, badgeColor, icon) = when (movement.type) {
        CashMovementType.INCOME -> Triple("Ingreso", EmeraldGreen, Icons.Default.CheckCircle)
        CashMovementType.EXPENSE -> Triple("Egreso", AccentYellowDark, Icons.Default.Warning)
        CashMovementType.ADJUSTMENT -> Triple("Ajuste", RubyRed, Icons.Default.Error)
    }

    val amountPrefix = if (movement.type == CashMovementType.INCOME) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hoy · ${movement.concept}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val company = Company.values().find { it.id == movement.companyId } ?: Company.ALL
                    if (company.iconRes != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = company.iconRes),
                            contentDescription = company.shortName,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    }

                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix C$ ${movement.amount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
