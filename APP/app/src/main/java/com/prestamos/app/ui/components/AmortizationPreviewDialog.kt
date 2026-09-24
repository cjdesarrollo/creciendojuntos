package com.prestamos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prestamos.app.domain.model.AmortizationSchedule
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AmortizationPreviewDialog(
    schedule: AmortizationSchedule,
    currency: CurrencyType,
    frequency: PaymentFrequency,
    clientName: String? = null,
    onDismiss: () -> Unit
) {
    val sym = currency.symbol
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val totalCount = schedule.installments.size.coerceAtLeast(1)
    val countBD = BigDecimal(totalCount)

    val capitalPerInst = schedule.principal.divide(countBD, 2, RoundingMode.HALF_UP)
    val interestPerInst = schedule.totalInterest.divide(countBD, 2, RoundingMode.HALF_UP)
    val extraFeePerInst = schedule.extraFeePerInstallment

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tabla de Amortización",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary
                            )
                            Text(
                                text = if (!clientName.isNullOrBlank()) "Cliente: $clientName" else "Plan de pagos proyectado",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumen superior en bloques
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Capital:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$sym ${schedule.principal}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Column {
                                Text("Interés (${schedule.interestPercent}%):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$sym ${schedule.totalInterest}", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                            Column {
                                Text("Otros Gastos:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$sym ${schedule.totalExtraFee}", fontWeight = FontWeight.Bold, color = AccentYellowDark)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monto Total:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$sym ${schedule.totalAmount}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderSlate)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${schedule.installments.size} Cuotas • Frecuencia: ${frequency.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Cuota Regular: $sym ${schedule.installmentAmount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabla de Desglose
                Text(
                    text = "Desglose por Cuota",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CardSurface, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Cabecera de la tabla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavyPrimary, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
                            Text("Vence", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                            Text("Capital", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.6f))
                            Text("Interés", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.6f))
                            Text("Otros", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                            Text("Cuota", fontWeight = FontWeight.Bold, color = AccentYellow, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.9f))
                        }

                        // Filas de las cuotas
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(schedule.installments) { index, inst ->
                                val rowBg = if (index % 2 == 0) LightSurface else Color.White
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(vertical = 9.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${inst.installmentNumber}", fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                    Text(dateFormat.format(Date(inst.dueDate)), color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                                    Text("$sym $capitalPerInst", color = TextPrimary, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.6f))
                                    Text("$sym $interestPerInst", color = TextPrimary, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.6f))
                                    Text("$sym $extraFeePerInst", color = TextPrimary, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                                    Text(
                                        "$sym ${inst.expectedAmount}",
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.9f)
                                    )
                                }
                                HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón de Cierre
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Cerrar Vista Previa", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
