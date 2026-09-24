package com.prestamos.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.Payment
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReceiptFormat(val displayName: String) {
    HALF_LETTER_LANDSCAPE("Media Carta (Horizontal)"),
    THERMAL_80MM("Ticket Térmico 80mm")
}

class PdfReceiptGenerator(
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) {

    fun generateReceiptPdf(
        client: Client,
        loan: Loan,
        payment: Payment,
        format: ReceiptFormat = ReceiptFormat.HALF_LETTER_LANDSCAPE
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val (width, height) = when (format) {
                ReceiptFormat.HALF_LETTER_LANDSCAPE -> Pair(612, 396)
                ReceiptFormat.THERMAL_80MM -> Pair(384, 600)
            }

            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            when (format) {
                ReceiptFormat.HALF_LETTER_LANDSCAPE -> drawHalfLetterHorizontal(canvas, client, loan, payment)
                ReceiptFormat.THERMAL_80MM -> draw80mmTicket(canvas, client, loan, payment)
            }

            pdfDocument.finishPage(page)

            val outputFile = fileStorageManager.createReceiptPdfFile(payment.id)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawHdLogo(
        canvas: Canvas,
        iconRes: Int?,
        containerX: Float,
        containerY: Float,
        containerW: Float,
        containerH: Float,
        paint: Paint
    ) {
        if (iconRes == null) return
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = true
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, iconRes, options) ?: return

        val padding = 4f
        val availableW = containerW - (padding * 2)
        val availableH = containerH - (padding * 2)

        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val ratio = srcW / srcH

        var dstW = availableW
        var dstH = availableW / ratio
        if (dstH > availableH) {
            dstH = availableH
            dstW = availableH * ratio
        }

        val dstLeft = containerX + (containerW - dstW) / 2f
        val dstTop = containerY + (containerH - dstH) / 2f

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = RectF(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH)

        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    private fun drawHalfLetterHorizontal(
        canvas: Canvas,
        client: Client,
        loan: Loan,
        payment: Payment
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val sym = loan.currency.symbol
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val company = Company.values().find { it.id == payment.companyId } ?: Company.CRECIENDO_JUNTOS
        val isCj = (company == Company.CRECIENDO_JUNTOS || company == Company.ALL)

        val phone = if (isCj) "8232 2262" else "8797 3321"

        // Fondo Blanco
        canvas.drawColor(Color.WHITE)

        if (isCj) {
            // ENCABEZADO CLARO CRECIENDO JUNTOS
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(0f, 0f, 612f, 65f, paint)

            paint.color = Color.parseColor("#D97706")
            canvas.drawRect(0f, 65f, 612f, 68f, paint)

            // Contenedor blanco para el Logo
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(20f, 8f, 75f, 57f), 8f, 8f, paint)
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(RectF(20f, 8f, 75f, 57f), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            drawHdLogo(canvas, company.iconRes, 20f, 8f, 55f, 49f, logoPaint)

            paint.color = Color.parseColor("#0A2540")
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textX = 85f
            canvas.drawText("Creciendo Juntos", textX, 28f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#D97706")
            canvas.drawText("MICROCRÉDITOS Y DESARROLLO • ATENCIÓN: $phone", textX, 42f, paint)

            paint.textSize = 10f
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Comprobante Oficial de Pago | Recibo #${payment.id}", textX, 56f, paint)

        } else {
            // ENCABEZADO AZUL CORPORATIVO FACILITO
            paint.color = Color.parseColor("#1E40AF")
            canvas.drawRect(0f, 0f, 612f, 65f, paint)

            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRect(0f, 65f, 612f, 68f, paint)

            // Contenedor blanco para el Logo
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(20f, 8f, 75f, 57f), 8f, 8f, paint)
            paint.color = Color.parseColor("#DBEAFE")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(RectF(20f, 8f, 75f, 57f), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            drawHdLogo(canvas, company.iconRes, 20f, 8f, 55f, 49f, logoPaint)

            paint.color = Color.WHITE
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textX = 85f
            canvas.drawText("Facilito", textX, 28f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#BAE6FD")
            canvas.drawText("CRÉDITOS RÁPIDOS Y SEGUROS • ATENCIÓN: $phone", textX, 42f, paint)

            paint.textSize = 10f
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Comprobante Oficial de Pago | Recibo #${payment.id}", textX, 56f, paint)
        }

        // Columna Izquierda: Información Cliente y Préstamo
        var y = 90f
        paint.color = if (isCj) Color.parseColor("#0A2540") else Color.parseColor("#1E40AF")
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("DATOS DEL CLIENTE Y PRÉSTAMO", 25f, y, paint)

        y += 12f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(25f, y, 300f, y, paint)

        y += 20f
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Cliente: ${client.fullName}", 25f, y, paint)
        y += 16f
        canvas.drawText("DNI / Cédula: ${client.dniOrId}", 25f, y, paint)
        y += 16f
        canvas.drawText("Teléfono: ${client.phone}", 25f, y, paint)
        y += 16f
        canvas.drawText("Préstamo Nro: #${loan.id} (${loan.currency.code})", 25f, y, paint)
        y += 16f
        canvas.drawText("Fecha Pago: ${dateFormat.format(Date(payment.paymentDate))}", 25f, y, paint)

        // Columna Derecha: Tarjeta de Pago Destacada
        val rightX = 320f
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(rightX, 80f, 587f, 210f), 8f, 8f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(rightX, 80f, 587f, 210f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isCj) Color.parseColor("#0A2540") else Color.parseColor("#1E40AF")
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("MONTO PAGADO:", rightX + 15f, 110f, paint)

        paint.textSize = 22f
        paint.color = Color.parseColor("#059669")
        canvas.drawText("$sym ${payment.amountPaid}", rightX + 15f, 142f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Tipo de Pago: ${payment.paymentType}", rightX + 15f, 168f, paint)
        canvas.drawText("Método: ${payment.paymentMethod.displayName}", rightX + 15f, 186f, paint)

        // Resumen del Estado de Cuenta (Pie de Página Horizontal)
        y = 240f
        paint.color = if (isCj) Color.parseColor("#0A2540") else Color.parseColor("#1E40AF")
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("ESTADO DE LA CUENTA", 25f, y, paint)

        y += 12f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(25f, y, 587f, y, paint)

        y += 22f
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Monto Total Acordado: $sym ${loan.totalAmountToPay}", 25f, y, paint)
        canvas.drawText("Total Cobrado a la Fecha: $sym ${loan.totalPaidAmount}", 220f, y, paint)

        val remaining = loan.totalAmountToPay.subtract(loan.totalPaidAmount).max(BigDecimal.ZERO)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = if (remaining > BigDecimal.ZERO) Color.parseColor("#DC2626") else Color.parseColor("#059669")
        canvas.drawText("Saldo Pendiente: $sym $remaining", 430f, y, paint)

        // Pie de Documento
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Documento de comprobante oficial de ${company.displayName} • Atención: $phone", 25f, 370f, paint)
    }

    private fun draw80mmTicket(
        canvas: Canvas,
        client: Client,
        loan: Loan,
        payment: Payment
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val sym = loan.currency.symbol
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val company = Company.values().find { it.id == payment.companyId } ?: Company.CRECIENDO_JUNTOS
        val isCj = (company == Company.CRECIENDO_JUNTOS || company == Company.ALL)
        val phone = if (isCj) "8232 2262" else "8797 3321"

        canvas.drawColor(Color.WHITE)

        var y = 20f
        if (company.iconRes != null) {
            drawHdLogo(canvas, company.iconRes, 142f, y, 100f, 60f, logoPaint)
            y += 70f
        }

        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(company.displayName, 192f, y, paint)

        y += 16f
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Teléfono de Contacto: $phone", 192f, y, paint)

        y += 14f
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        canvas.drawText("Comprobante de Pago Térmico", 192f, y, paint)

        y += 15f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.GRAY
        canvas.drawLine(20f, y, 364f, y, paint)

        y += 20f
        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("Recibo Nro: #${payment.id}", 20f, y, paint)
        y += 15f
        canvas.drawText("Fecha: ${dateFormat.format(Date(payment.paymentDate))}", 20f, y, paint)
        y += 15f
        canvas.drawText("Cliente: ${client.fullName}", 20f, y, paint)
        y += 15f
        canvas.drawText("DNI/ID: ${client.dniOrId}", 20f, y, paint)
        y += 15f
        canvas.drawText("Préstamo #: ${loan.id} (${loan.currency.code})", 20f, y, paint)

        y += 15f
        canvas.drawLine(20f, y, 364f, y, paint)

        y += 25f
        paint.textSize = 13f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.parseColor("#059669")
        canvas.drawText("MONTO PAGADO: $sym ${payment.amountPaid}", 20f, y, paint)

        y += 18f
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.BLACK
        canvas.drawText("Tipo: ${payment.paymentType}", 20f, y, paint)
        y += 15f
        canvas.drawText("Método: ${payment.paymentMethod.displayName}", 20f, y, paint)

        y += 15f
        canvas.drawLine(20f, y, 364f, y, paint)

        y += 20f
        val remaining = loan.totalAmountToPay.subtract(loan.totalPaidAmount).max(BigDecimal.ZERO)
        canvas.drawText("Monto Total Crédito: $sym ${loan.totalAmountToPay}", 20f, y, paint)
        y += 15f
        canvas.drawText("Total Pagado Acumulado: $sym ${loan.totalPaidAmount}", 20f, y, paint)
        y += 15f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = if (remaining > BigDecimal.ZERO) Color.RED else Color.parseColor("#059669")
        canvas.drawText("Saldo Pendiente: $sym $remaining", 20f, y, paint)

        y += 30f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("¡Gracias por su puntual pago!", 192f, y, paint)
        y += 14f
        canvas.drawText("Servicio al cliente: $phone", 192f, y, paint)
    }
}
