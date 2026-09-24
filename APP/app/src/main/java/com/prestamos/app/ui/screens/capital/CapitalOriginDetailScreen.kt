package com.prestamos.app.ui.screens.capital

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prestamos.app.domain.model.CapitalTransaction
import com.prestamos.app.ui.viewmodel.CapitalOriginViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapitalOriginDetailScreen(
    originId: Long,
    viewModel: CapitalOriginViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedOrigin by viewModel.selectedOrigin.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf("INGRESO") }

    LaunchedEffect(originId) {
        viewModel.selectOrigin(originId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedOrigin?.name ?: "Detalle de Origen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Saldo Actual",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${selectedOrigin?.currentBalance ?: BigDecimal.ZERO} NIO", // Asumiendo NIO por defecto, podría ser dinámico
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    transactionType = "INGRESO"
                    showTransactionDialog = true
                }) {
                    Text("Ingresar Capital")
                }
                Button(
                    onClick = {
                        transactionType = "EGRESO"
                        showTransactionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Retirar Capital")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Historial de Transacciones", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { tx ->
                    TransactionCard(tx)
                }
            }
        }
    }

    if (showTransactionDialog) {
        var amountText by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text(if (transactionType == "INGRESO") "Ingreso de Capital" else "Retiro de Capital") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (Opcional)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountText.toBigDecimalOrNull()
                    if (amount != null && amount > BigDecimal.ZERO) {
                        viewModel.addTransaction(originId, transactionType, amount, notes)
                        showTransactionDialog = false
                    }
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransactionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(transaction: CapitalTransaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = transaction.type, style = MaterialTheme.typography.titleSmall)
                if (!transaction.notes.isNullOrBlank()) {
                    Text(text = transaction.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = "${if (transaction.type == "INGRESO") "+" else "-"}${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == "INGRESO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
