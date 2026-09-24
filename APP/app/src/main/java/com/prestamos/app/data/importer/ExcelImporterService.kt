package com.prestamos.app.data.importer

import android.content.Context
import android.net.Uri
import com.prestamos.app.data.local.db.dao.ClientDao
import com.prestamos.app.data.local.db.dao.LoanDao
import com.prestamos.app.data.local.db.entities.ClientEntity
import com.prestamos.app.data.local.db.entities.InstallmentEntity
import com.prestamos.app.data.local.db.entities.LoanEntity
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.PaymentFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelImporterService(
    private val context: Context,
    private val clientDao: ClientDao,
    private val loanDao: LoanDao
) {
    suspend fun importFromExcel(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("No se pudo abrir el archivo.")

            val workbook = WorkbookFactory.create(inputStream)

            // Import Clients
            val clientsSheet = workbook.getSheet("Clientes") ?: throw Exception("Falta la hoja 'Clientes'")
            val clientsMap = mutableMapOf<String, Long>() // DNI to ClientId

            for (i in 1..clientsSheet.lastRowNum) {
                val row = clientsSheet.getRow(i) ?: continue
                val dni = getStringValue(row, 0)
                val nombre = getStringValue(row, 1)
                val telefono = getStringValue(row, 2)
                val direccion = getStringValue(row, 3)

                if (dni.isNotBlank() && nombre.isNotBlank()) {
                    val client = ClientEntity(
                        nombreCompleto = nombre,
                        dniOIdentificacion = dni,
                        telefono = telefono,
                        direccion = direccion
                    )
                    val id = clientDao.insertClient(client)
                    clientsMap[dni] = id
                }
            }

            // Import Loans
            val loansSheet = workbook.getSheet("Prestamos") ?: throw Exception("Falta la hoja 'Prestamos'")
            val loansMap = mutableMapOf<String, Long>() // Some unique ID (like "DNI_PRESTAMOID") to LoanId

            for (i in 1..loansSheet.lastRowNum) {
                val row = loansSheet.getRow(i) ?: continue
                val prestamoRefId = getStringValue(row, 0) // Can be anything to link installments
                val dniCliente = getStringValue(row, 1)
                val monto = getBigDecimalValue(row, 2)
                val tasa = getBigDecimalValue(row, 3)
                val frecuencia = getStringValue(row, 4)
                val totalCuotas = getNumericValue(row, 5).toInt()
                val fechaInicio = getDateValue(row, 6)

                val clientId = clientsMap[dniCliente]
                if (clientId != null && monto > BigDecimal.ZERO) {
                    val loan = LoanEntity(
                        clienteId = clientId,
                        montoPrincipal = monto,
                        tasaInteresPorcentaje = tasa,
                        montoTotalInteres = monto.multiply(tasa.divide(BigDecimal(100))),
                        montoTotalAPagar = monto.add(monto.multiply(tasa.divide(BigDecimal(100)))), // Simplified
                        frecuenciaPago = parseFrecuencia(frecuencia),
                        totalCuotas = totalCuotas,
                        montoCuota = monto.add(monto.multiply(tasa.divide(BigDecimal(100)))).divide(BigDecimal(totalCuotas.coerceAtLeast(1)), 2, java.math.RoundingMode.HALF_UP),
                        fechaInicio = fechaInicio,
                        fechaVencimiento = fechaInicio + (totalCuotas * 7L * 24 * 60 * 60 * 1000) // Rough estimate
                    )
                    val id = loanDao.insertLoan(loan)
                    loansMap[prestamoRefId] = id
                }
            }

            // Import Installments
            val installmentsSheet = workbook.getSheet("Cuotas")
            if (installmentsSheet != null) {
                val installmentsToInsert = mutableListOf<InstallmentEntity>()
                for (i in 1..installmentsSheet.lastRowNum) {
                    val row = installmentsSheet.getRow(i) ?: continue
                    val prestamoRefId = getStringValue(row, 0)
                    val numeroCuota = getNumericValue(row, 1).toInt()
                    val fechaVencimiento = getDateValue(row, 2)
                    val montoEsperado = getBigDecimalValue(row, 3)

                    val loanId = loansMap[prestamoRefId]
                    if (loanId != null) {
                        installmentsToInsert.add(
                            InstallmentEntity(
                                prestamoId = loanId,
                                numeroCuota = numeroCuota,
                                fechaVencimiento = fechaVencimiento,
                                montoEsperado = montoEsperado
                            )
                        )
                    }
                }
                if (installmentsToInsert.isNotEmpty()) {
                    loanDao.insertInstallments(installmentsToInsert)
                }
            }

            workbook.close()
            inputStream.close()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseFrecuencia(frecuencia: String): PaymentFrequency {
        return when (frecuencia.uppercase()) {
            "DIARIO" -> PaymentFrequency.DAILY
            "SEMANAL" -> PaymentFrequency.WEEKLY
            "QUINCENAL" -> PaymentFrequency.BIWEEKLY
            "MENSUAL" -> PaymentFrequency.MONTHLY
            else -> PaymentFrequency.MONTHLY
        }
    }

    private fun getStringValue(row: Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            else -> ""
        }
    }

    private fun getNumericValue(row: Row, index: Int): Double {
        val cell = row.getCell(index) ?: return 0.0
        return if (cell.cellType == CellType.NUMERIC) cell.numericCellValue else 0.0
    }

    private fun getBigDecimalValue(row: Row, index: Int): BigDecimal {
        val num = getNumericValue(row, index)
        return BigDecimal.valueOf(num)
    }

    private fun getDateValue(row: Row, index: Int): Long {
        val cell = row.getCell(index) ?: return System.currentTimeMillis()
        return if (cell.cellType == CellType.NUMERIC) {
            cell.dateCellValue?.time ?: System.currentTimeMillis()
        } else {
            // Try parsing string if it's not a proper date cell
            try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                format.parse(cell.stringCellValue)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
