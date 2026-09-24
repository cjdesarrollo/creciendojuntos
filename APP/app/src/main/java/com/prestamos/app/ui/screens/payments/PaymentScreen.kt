package com.prestamos.app.ui.screens.payments

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.prestamos.app.data.export.ReceiptFormat
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.PaymentMethod
import com.prestamos.app.ui.theme.AccentBlue
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.AmberLight
import com.prestamos.app.ui.theme.BorderSlate
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.EmeraldGreen
import com.prestamos.app.ui.theme.EmeraldLight
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.NavyPrimary
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.RubyLight
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import com.prestamos.app.ui.viewmodel.PaymentViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentScreen(
    loanId: Long,
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(loanId) {
        viewModel.loadLoanDetails(loanId)
    }

    val loan = state.selectedLoan
    val client = state.selectedClient
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold { innerPadding ->
        if (state.isPaymentSuccess) {
            // Pantalla de Recibo y Opciones de Impresión
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightSurface)
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡Pago Registrado Exitosamente!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text("Monto Recaudado: ${loan?.currency?.symbol} ${state.lastRecordedPayment?.amountPaid}", style = MaterialTheme.typography.titleMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Formato de Impresión / Recibo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReceiptFormat.values().forEach { fmt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onReceiptFormatChanged(fmt) }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.receiptFormat == fmt,
                                    onClick = { viewModel.onReceiptFormatChanged(fmt) }
                                )
                                Text(fmt.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Compartir / Abrir PDF
                    Button(
                        onClick = {
                            state.shareableContentUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir Recibo PDF"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir PDF")
                    }

                    // Finalizar y Volver
                    Button(
                        onClick = {
                            viewModel.resetPaymentState()
                            onBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        } else {
            // Formulario de Pago
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
                    Text("Cobrar / Aplicar Pago", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                }

                if (loan != null && client != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(client.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("DNI: ${client.dniOrId} • Préstamo #${loan.id}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Pendiente:", color = TextSecondary)
                                Text("${loan.currency.symbol} ${loan.remainingBalance}", fontWeight = FontWeight.Bold, color = RubyRed)
                            }
                        }
                    }

                    // Modalidad de Pago (Cuota Corriente, Adelantar Cuota, Abono al Capital)
                    Text("Tipo de Operación de Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Triple("CUOTA", "Pagar Cuota Corriente", "Satisface la cuota actual pendiente"),
                            Triple("ADELANTO", "Adelantar Cuota(s)", "Cubre cuotas futuras por adelantado"),
                            Triple("CAPITAL", "Abono Directo al Capital", "Reduce el saldo principal sin afectar comisión de cuota")
                        ).forEach { (typeKey, title, subtitle) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onPaymentTypeChanged(typeKey) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (state.paymentType == typeKey) NavyPrimary.copy(alpha = 0.08f) else CardSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = state.paymentType == typeKey, onClick = { viewModel.onPaymentTypeChanged(typeKey) })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Indicador de Cuota Específica Seleccionada
                    val selectedInst = state.installments.find { it.id == state.selectedInstallmentId }
                    if (selectedInst != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.5.dp, PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "🎯 Cancelando Cuota #${selectedInst.installmentNumber}",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                    Text(
                                        "Saldo de cuota: ${loan.currency.symbol} ${selectedInst.remainingAmount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { viewModel.clearSelectedInstallment() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar selección",
                                        tint = RubyRed
                                    )
                                }
                            }
                        }
                    }

                    // Campo de Monto a Cobrar
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = { viewModel.onAmountInputChanged(it) },
                        label = { Text("Monto a Ingresar (${loan.currency.symbol}) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Método de Pago
                    Text("Método de Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PaymentMethod.values().forEach { method ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 24.dp)
                                    .clickable { viewModel.onPaymentMethodChanged(method) }
                            ) {
                                RadioButton(selected = state.paymentMethod == method, onClick = { viewModel.onPaymentMethodChanged(method) })
                                Text(method.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    state.errorMessage?.let { err ->
                        Text(err, color = RubyRed, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.recordPayment() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aplicar Pago e Imprimir Recibo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    // Cronograma de Cuotas con Selección Interactiva y Colores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cronograma de Cuotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("Toca una cuota para cancelarla", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (state.selectedInstallmentId != null) {
                            TextButton(onClick = { viewModel.clearSelectedInstallment() }) {
                                Text("Pago General", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }

                    // Leyenda Visual de Colores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentStatusChip("Pagada", EmeraldGreen, EmeraldLight)
                        PaymentStatusChip("Pendiente", PrimaryBlue, LightSurface)
                        PaymentStatusChip("Retrasada", RubyRed, RubyLight)
                    }

                    val now = System.currentTimeMillis()
                    state.installments.forEach { inst ->
                        val isPaid = inst.status == InstallmentStatus.PAID || inst.remainingAmount <= BigDecimal.ZERO
                        val isOverdue = !isPaid && (inst.status == InstallmentStatus.OVERDUE || inst.dueDate < now)
                        val isPartial = !isPaid && !isOverdue && inst.paidAmount > BigDecimal.ZERO
                        val isSelected = state.selectedInstallmentId == inst.id

                        val (statusText, statusColor, statusBg) = when {
                            isPaid -> Triple("PAGADA", EmeraldGreen, EmeraldLight)
                            isOverdue -> Triple("RETRASADA", RubyRed, RubyLight)
                            isPartial -> Triple("PARCIAL", AccentYellowDark, AmberLight)
                            else -> Triple("PENDIENTE", PrimaryBlue, LightSurface)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isPaid) {
                                    viewModel.onSelectInstallment(inst)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else CardSurface
                            ),
                            border = when {
                                isSelected -> BorderStroke(2.dp, PrimaryBlue)
                                isOverdue -> BorderStroke(1.dp, RubyRed.copy(alpha = 0.5f))
                                isPaid -> BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                                else -> BorderStroke(1.dp, BorderSlate)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isPaid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (isOverdue) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = RubyRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.onSelectInstallment(inst) },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Cuota #${inst.installmentNumber}",
                                                fontWeight = FontWeight.Bold,
                                                color = NavyPrimary
                                            )
                                            Surface(
                                                color = statusBg,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Vence: ${dateFormat.format(Date(inst.dueDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOverdue) RubyRed else TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${loan.currency.symbol} ${if (isPaid) inst.expectedAmount else inst.remainingAmount}",
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    if (!isPaid && inst.paidAmount > BigDecimal.ZERO) {
                                        Text(
                                            "Abonado: ${loan.currency.symbol} ${inst.paidAmount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
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
private fun PaymentStatusChip(text: String, color: Color, background: Color) {
    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
