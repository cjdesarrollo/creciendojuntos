package com.prestamos.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prestamos.app.domain.model.PaymentFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFrequencyDropdownMenu(
    selectedFrequency: PaymentFrequency,
    onFrequencySelected: (PaymentFrequency) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Frecuencia de Pago *"
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFrequency.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PaymentFrequency.values().forEach { freq ->
                val subtitle = when (freq) {
                    PaymentFrequency.MONTHLY -> "1 cuota al mes"
                    PaymentFrequency.BIWEEKLY -> "2 cuotas al mes (cada 15 días)"
                    PaymentFrequency.WEEKLY -> "4 cuotas al mes (cada 7 días)"
                    PaymentFrequency.DAILY -> "30 cuotas al mes (diario)"
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${freq.displayName} ($subtitle)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onFrequencySelected(freq)
                        expanded = false
                    }
                )
            }
        }
    }
}
