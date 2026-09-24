package com.prestamos.app.data.export

import android.content.Context
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.Payment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExcelCsvExporter(
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "DO"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /**
     * Exporta el reporte de clientes y préstamos en formato estructurado Excel XML / .xlsx
     */
    fun exportLoansToExcel(loans: List<Loan>, clientsMap: Map<Long, Client>): File? {
        return try {
            val file = fileStorageManager.createReportExcelFile("Reporte_Prestamos")
            val xmlContent = StringBuilder()

            xmlContent.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            xmlContent.append("""<?mso-application progid="Excel.Sheet"?>""").append("\n")
            xmlContent.append("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" """)
            xmlContent.append("""xmlns:o="urn:schemas-microsoft-com:office:office" """)
            xmlContent.append("""xmlns:x="urn:schemas-microsoft-com:office:excel" """)
            xmlContent.append("""xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""").append("\n")
            
            xmlContent.append("<Styles>\n")
            xmlContent.append("""<Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0A192F" ss:Pattern="Solid"/></Style>""").append("\n")
            xmlContent.append("""<Style ss:ID="Currency"><NumberFormat ss:Format="${'$'}#,##0.00"/></Style>""").append("\n")
            xmlContent.append("</Styles>\n")

            xmlContent.append("""<Worksheet ss:Name="Prestamos">""").append("\n")
            xmlContent.append("<Table>\n")

            // Encabezados
            xmlContent.append("<Row>\n")
            val headers = listOf("ID Préstamo", "Cliente", "DNI/Cédula", "Teléfono", "Moneda", "Capital", "% Interés", "Total Interés", "Total A Pagar", "Cuota", "Frecuencia", "Total Pagado", "Saldo Restante", "Estado", "Fecha Inicio")
            headers.forEach { h ->
                xmlContent.append("""<Cell ss:StyleID="Header"><Data ss:Type="String">$h</Data></Cell>""").append("\n")
            }
            xmlContent.append("</Row>\n")

            // Filas de datos
            loans.forEach { loan ->
                val client = clientsMap[loan.clientId]
                val clientName = client?.fullName ?: loan.clientName
                val clientDni = client?.dniOrId ?: ""
                val clientPhone = client?.phone ?: ""
                val remaining = loan.totalAmountToPay.subtract(loan.totalPaidAmount).max(java.math.BigDecimal.ZERO)

                xmlContent.append("<Row>\n")
                xmlContent.append("""<Cell><Data ss:Type="Number">${loan.id}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientName)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientDni)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientPhone)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${loan.currency.code}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${loan.principalAmount}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="Number">${loan.interestRatePercent}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${loan.totalInterestAmount}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${loan.totalAmountToPay}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${loan.installmentAmount}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${loan.paymentFrequency.displayName}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${loan.totalPaidAmount}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">$remaining</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${loan.status.displayName}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${dateFormat.format(Date(loan.startDate))}</Data></Cell>""").append("\n")
                xmlContent.append("</Row>\n")
            }


            xmlContent.append("</Table>\n")
            xmlContent.append("</Worksheet>\n")
            xmlContent.append("</Workbook>\n")

            FileOutputStream(file).use { out ->
                out.write(xmlContent.toString().toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exporta el historial de cobros a CSV
     */
    fun exportPaymentsToCsv(payments: List<Payment>, loansMap: Map<Long, Loan>, clientsMap: Map<Long, Client>): File? {
        return try {
            val file = File(context.cacheDir, "Cobros_${System.currentTimeMillis()}.csv")
            val sb = StringBuilder()
            sb.append("ID Pago,ID Préstamo,Cliente,Monto Pagado,Método,Fecha Pago,Notas\n")

            payments.forEach { p ->
                val loan = loansMap[p.loanId]
                val client = loan?.let { clientsMap[it.clientId] }
                val clientName = client?.fullName ?: "Desconocido"

                sb.append("${p.id},${p.loanId},\"${escapeCsv(clientName)}\",${p.amountPaid},${p.paymentMethod.displayName},\"${dateFormat.format(Date(p.paymentDate))}\",\"${escapeCsv(p.notes ?: "")}\"\n")
            }

            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exporta el historial de pagos/cobros filtrados a formato Excel .xlsx (Spreadsheet XML)
     */
    fun exportPaymentsToExcel(
        payments: List<Payment>,
        loansMap: Map<Long, Loan>,
        clientsMap: Map<Long, Client>
    ): File? {
        return try {
            val file = fileStorageManager.createReportExcelFile("Reporte_Pagos")
            val xmlContent = StringBuilder()

            xmlContent.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            xmlContent.append("""<?mso-application progid="Excel.Sheet"?>""").append("\n")
            xmlContent.append("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" """)
            xmlContent.append("""xmlns:o="urn:schemas-microsoft-com:office:office" """)
            xmlContent.append("""xmlns:x="urn:schemas-microsoft-com:office:excel" """)
            xmlContent.append("""xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""").append("\n")

            xmlContent.append("<Styles>\n")
            xmlContent.append("""<Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0A2540" ss:Pattern="Solid"/></Style>""").append("\n")
            xmlContent.append("""<Style ss:ID="Currency"><NumberFormat ss:Format="C$#,##0.00"/></Style>""").append("\n")
            xmlContent.append("""<Style ss:ID="Total"><Font ss:Bold="1" ss:Color="#0A2540"/><Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/><NumberFormat ss:Format="C$#,##0.00"/></Style>""").append("\n")
            xmlContent.append("</Styles>\n")

            xmlContent.append("""<Worksheet ss:Name="Historial_Pagos">""").append("\n")
            xmlContent.append("<Table>\n")

            // Encabezados
            xmlContent.append("<Row>\n")
            val headers = listOf("N° Recibo", "Fecha y Hora", "Cliente", "Cédula / DNI", "Teléfono", "Préstamo #", "Empresa", "Monto Pagado", "Método", "Tipo de Pago", "Notas")
            headers.forEach { h ->
                xmlContent.append("""<Cell ss:StyleID="Header"><Data ss:Type="String">$h</Data></Cell>""").append("\n")
            }
            xmlContent.append("</Row>\n")

            var totalAmount = java.math.BigDecimal.ZERO

            // Filas de datos
            payments.forEach { p ->
                val loan = loansMap[p.loanId]
                val client = loan?.let { clientsMap[it.clientId] }
                val clientName = client?.fullName ?: "Cliente #${p.loanId}"
                val clientDni = client?.dniOrId ?: ""
                val clientPhone = client?.phone ?: ""
                val companyName = if (p.companyId == 2L) "Facilito" else "Creciendo Juntos"
                totalAmount = totalAmount.add(p.amountPaid)

                xmlContent.append("<Row>\n")
                xmlContent.append("""<Cell><Data ss:Type="Number">${p.id}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${dateFormat.format(Date(p.paymentDate))}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientName)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientDni)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(clientPhone)}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="Number">${p.loanId}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">$companyName</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell ss:StyleID="Currency"><Data ss:Type="Number">${p.amountPaid}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${p.paymentMethod.displayName}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${p.paymentType}</Data></Cell>""").append("\n")
                xmlContent.append("""<Cell><Data ss:Type="String">${escapeXml(p.notes ?: "")}</Data></Cell>""").append("\n")
                xmlContent.append("</Row>\n")
            }

            // Fila de Total
            xmlContent.append("<Row>\n")
            xmlContent.append("""<Cell ss:StyleID="Total"><Data ss:Type="String">TOTAL RECAUDADO</Data></Cell>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"><Data ss:Type="Number">$totalAmount</Data></Cell>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("""<Cell ss:StyleID="Total"/>""").append("\n")
            xmlContent.append("</Row>\n")

            xmlContent.append("</Table>\n")
            xmlContent.append("</Worksheet>\n")
            xmlContent.append("</Workbook>\n")

            FileOutputStream(file).use { out ->
                out.write(xmlContent.toString().toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crea una copia de respaldo completa cifrada (.backup) que incluye la base de datos y adjuntos.
     */
    fun createFullBackup(targetFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("prestamos_encrypted.db")
            val filesDir = fileStorageManager.getFilesDirectory()

            ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                if (dbFile.exists()) {
                    addFileToZip(dbFile, "db/prestamos_encrypted.db", zos)
                }
                addDirectoryToZip(filesDir, "files/", zos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restaura la base de datos y archivos desde un paquete de respaldo (.backup).
     */
    fun restoreFullBackup(backupFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("prestamos_encrypted.db")
            val filesDir = fileStorageManager.getFilesDirectory()

            ZipInputStream(FileInputStream(backupFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName.startsWith("db/")) {
                        FileOutputStream(dbFile).use { out -> zis.copyTo(out) }
                    } else if (entryName.startsWith("files/")) {
                        val relativePath = entryName.removePrefix("files/")
                        if (relativePath.isNotEmpty()) {
                            val destFile = File(filesDir, relativePath)
                            if (entry.isDirectory) {
                                destFile.mkdirs()
                            } else {
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { out -> zis.copyTo(out) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Exporta la base de datos actual a un archivo independiente en formato .db3.
     */
    fun exportDb3File(targetFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("prestamos_encrypted.db")
            if (!dbFile.exists()) return false

            FileInputStream(dbFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restaura la base de datos de la aplicación a partir de un archivo .db3 externo.
     * Limpia los archivos WAL y SHM para garantizar la consistencia.
     */
    fun restoreDb3File(db3File: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("prestamos_encrypted.db")
            val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
            val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")

            // Copiar el archivo DB3 al archivo de base de datos de la app
            FileInputStream(db3File).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Eliminar journals temporales anteriores para evitar sobreescrituras inconsistentes
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, zipPath: String, zos: ZipOutputStream) {
        val entry = ZipEntry(zipPath)
        zos.putNextEntry(entry)
        FileInputStream(file).use { input -> input.copyTo(zos) }
        zos.closeEntry()
    }

    private fun addDirectoryToZip(dir: File, baseZipPath: String, zos: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            val zipPath = "$baseZipPath${file.name}"
            if (file.isDirectory) {
                addDirectoryToZip(file, "$zipPath/", zos)
            } else {
                addFileToZip(file, zipPath, zos)
            }
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
