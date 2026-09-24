package com.prestamos.app.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileStorageManager(private val context: Context) {

    private val docsDir: File by lazy {
        File(context.filesDir, "client_docs").apply { if (!exists()) mkdirs() }
    }

    private val signaturesDir: File by lazy {
        File(context.filesDir, "signatures").apply { if (!exists()) mkdirs() }
    }

    private val receiptsDir: File by lazy {
        File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
    }

    private val reportsDir: File by lazy {
        File(context.filesDir, "reports").apply { if (!exists()) mkdirs() }
    }

    /**
     * Copia una imagen/archivo desde un Uri externo al almacenamiento privado `context.filesDir`.
     * Retorna la ruta absoluta interna guardada.
     */
    fun saveImageToPrivateStorage(sourceUri: Uri, prefix: String): String? {
        return try {
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(docsDir, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(targetFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda un Bitmap de firma digital trazada en Compose Canvas como PNG privado.
     */
    fun saveSignatureBitmap(bitmap: Bitmap, clientId: Long): String? {
        return try {
            val fileName = "sig_${clientId}_${System.currentTimeMillis()}.png"
            val targetFile = File(signaturesDir, fileName)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crea un archivo privado de recibo PDF.
     */
    fun createReceiptPdfFile(paymentId: Long): File {
        return File(receiptsDir, "recibo_pago_$paymentId.pdf")
    }

    /**
     * Crea un archivo privado de tabla de amortización PDF.
     */
    fun createAmortizationPdfFile(identifier: String): File {
        return File(reportsDir, "amortizacion_${identifier}_${System.currentTimeMillis()}.pdf")
    }

    /**
     * Crea un archivo privado de reporte Excel .xlsx.
     */
    fun createReportExcelFile(reportName: String): File {
        return File(reportsDir, "${reportName}_${System.currentTimeMillis()}.xlsx")
    }

    /**
     * Genera un Uri seguro con FileProvider para compartir via Intent.
     */
    fun getShareableContentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "com.prestamos.app.fileprovider",
            file
        )
    }

    fun getFilesDirectory(): File = context.filesDir
}
