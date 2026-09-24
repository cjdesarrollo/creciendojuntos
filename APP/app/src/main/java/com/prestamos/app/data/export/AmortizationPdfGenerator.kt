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
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.Installment
import com.prestamos.app.domain.model.Loan
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AmortizationPdfGenerator(
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) {

    fun generateAmortizationPdf(
        clientName: String,
        clientDni: String,
        loan: Loan,
        installments: List<Installment>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create() // Letter Size (8.5 x 11 in)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawAmortizationSheet(canvas, clientName, clientDni, loan, installments)

            pdfDocument.finishPage(page)

            val identifier = if (loan.id > 0) "prestamo_${loan.id}" else "simulacion"
            val outputFile = fileStorageManager.createAmortizationPdfFile(identifier)
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

    private fun drawAmortizationSheet(
        canvas: Canvas,
        clientName: String,
        clientDni: String,
        loan: Loan,
        installments: List<Installment>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val sym = loan.currency.symbol
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val company = Company.values().find { it.id == loan.companyId } ?: Company.CRECIENDO_JUNTOS
        val isCj = (company == Company.CRECIENDO_JUNTOS || company == Company.ALL)

        val phone = if (isCj) "8232 2262" else "8797 3321"

        // Fondo de toda la página
        canvas.drawColor(Color.WHITE)

        if (isCj) {
            // ==========================================
            // DISEÑO CRECIENDO JUNTOS: TONO CLARO ELEGANTE
            // ==========================================
            // Encabezado Claro
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(0f, 0f, 612f, 95f, paint)

            // Borde inferior sutil y franja dorada
            paint.color = Color.parseColor("#D97706")
            canvas.drawRect(0f, 95f, 612f, 99f, paint)

            // Tarjeta blanca para el Logo con marco dorado
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(35f, 12f, 120f, 85f), 12f, 12f, paint)
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(RectF(35f, 12f, 120f, 85f), 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            // Dibujar Logo HD
            drawHdLogo(canvas, company.iconRes, 35f, 12f, 85f, 73f, logoPaint)

            // Textos del Encabezado (Azul Marino + Dorado)
            val textX = 135f
            paint.color = Color.parseColor("#0A2540")
            paint.textSize = 21f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Creciendo Juntos", textX, 38f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#D97706")
            canvas.drawText("MICROCRÉDITOS Y DESARROLLO • ATENCIÓN: $phone", textX, 55f, paint)

            paint.textSize = 11f
            paint.color = Color.parseColor("#334155")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TABLA DE AMORTIZACIÓN Y PLAN DE PAGOS", textX, 74f, paint)

            drawBodyContent(
                canvas = canvas,
                paint = paint,
                clientName = clientName,
                clientDni = clientDni,
                loan = loan,
                installments = installments,
                sym = sym,
                dateFormat = dateFormat,
                primaryColor = Color.parseColor("#0A2540"),
                accentColor = Color.parseColor("#D97706"),
                companyDisplayName = company.displayName,
                phone = phone
            )

        } else {
            // ==========================================
            // DISEÑO FACILITO: AZUL CORPORATIVO VIBRANTE
            // ==========================================
            // Encabezado Azul
            paint.color = Color.parseColor("#1E40AF")
            canvas.drawRect(0f, 0f, 612f, 95f, paint)

            // Franja de acento azul cielo
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRect(0f, 95f, 612f, 99f, paint)

            // Tarjeta blanca para el Logo
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(35f, 12f, 120f, 85f), 12f, 12f, paint)
            paint.color = Color.parseColor("#DBEAFE")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(RectF(35f, 12f, 120f, 85f), 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            // Dibujar Logo HD
            drawHdLogo(canvas, company.iconRes, 35f, 12f, 85f, 73f, logoPaint)

            // Textos del Encabezado (Blanco + Azul Cielo)
            val textX = 135f
            paint.color = Color.WHITE
            paint.textSize = 21f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Facilito", textX, 38f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#BAE6FD")
            canvas.drawText("CRÉDITOS RÁPIDOS Y ACCESIBLES • ATENCIÓN: $phone", textX, 55f, paint)

            paint.textSize = 11f
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TABLA DE AMORTIZACIÓN Y PLAN DE PAGOS", textX, 74f, paint)

            drawBodyContent(
                canvas = canvas,
                paint = paint,
                clientName = clientName,
                clientDni = clientDni,
                loan = loan,
                installments = installments,
                sym = sym,
                dateFormat = dateFormat,
                primaryColor = Color.parseColor("#1E40AF"),
                accentColor = Color.parseColor("#0284C7"),
                companyDisplayName = company.displayName,
                phone = phone
            )
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

        val padding = 6f
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

    private fun drawBodyContent(
        canvas: Canvas,
        paint: Paint,
        clientName: String,
        clientDni: String,
        loan: Loan,
        installments: List<Installment>,
        sym: String,
        dateFormat: SimpleDateFormat,
        primaryColor: Int,
        accentColor: Int,
        companyDisplayName: String,
        phone: String
    ) {
        // Resumen del Cliente y Préstamo
        var y = 130f
        paint.color = primaryColor
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("DATOS GENERALES DEL CRÉDITO", 35f, y, paint)

        y += 12f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(35f, y, 577f, y, paint)

        val totalInst = loan.totalInstallments.coerceAtLeast(1)
        val capitalPerInstallment = loan.principalAmount.divide(BigDecimal(totalInst), 2, RoundingMode.HALF_UP)
        val interestPerInstallment = loan.totalInterestAmount.divide(BigDecimal(totalInst), 2, RoundingMode.HALF_UP)
        val otherFeesTotal = loan.totalAmountToPay.subtract(loan.principalAmount).subtract(loan.totalInterestAmount).max(BigDecimal.ZERO)
        val otherPerInstallment = otherFeesTotal.divide(BigDecimal(totalInst), 2, RoundingMode.HALF_UP)

        y += 20f
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Cliente: $clientName", 35f, y, paint)
        canvas.drawText("Moneda: ${loan.currency.displayName}", 320f, y, paint)

        y += 16f
        canvas.drawText("DNI / Identificación: $clientDni", 35f, y, paint)
        canvas.drawText("Capital Inicial: $sym ${loan.principalAmount}", 320f, y, paint)

        y += 16f
        canvas.drawText("Frecuencia: ${loan.paymentFrequency.displayName}", 35f, y, paint)
        canvas.drawText("Interés Total (${loan.interestRatePercent}%): $sym ${loan.totalInterestAmount}", 320f, y, paint)

        y += 16f
        canvas.drawText("Total de Cuotas: ${loan.totalInstallments}", 35f, y, paint)
        canvas.drawText("Otros Gastos / Seguros: $sym $otherFeesTotal", 320f, y, paint)

        y += 16f
        canvas.drawText("Fecha de Emisión: ${dateFormat.format(Date(loan.startDate))}", 35f, y, paint)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = accentColor
        canvas.drawText("Monto por Cuota: $sym ${loan.installmentAmount}", 320f, y, paint)

        // Tabla de Desglose de Cuotas (con columna OTROS GASTOS)
        y += 25f
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(35f, y, 577f, y + 26f), 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("#", 45f, y + 17f, paint)
        canvas.drawText("Fecha Venc.", 75f, y + 17f, paint)
        canvas.drawText("Capital", 160f, y + 17f, paint)
        canvas.drawText("Interés", 240f, y + 17f, paint)
        canvas.drawText("Otros Gastos", 320f, y + 17f, paint)
        canvas.drawText("Cuota Total", 410f, y + 17f, paint)
        canvas.drawText("Estado", 505f, y + 17f, paint)

        y += 26f
        paint.typeface = Typeface.DEFAULT

        installments.forEachIndexed { index, inst ->
            if (y > 740f) return@forEachIndexed

            val rowBg = if (index % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            paint.color = rowBg
            canvas.drawRect(35f, y, 577f, y + 20f, paint)

            paint.color = Color.BLACK
            paint.textSize = 9f
            canvas.drawText("${inst.installmentNumber}", 45f, y + 14f, paint)
            canvas.drawText(dateFormat.format(Date(inst.dueDate)), 75f, y + 14f, paint)
            canvas.drawText("$sym $capitalPerInstallment", 160f, y + 14f, paint)
            canvas.drawText("$sym $interestPerInstallment", 240f, y + 14f, paint)
            canvas.drawText("$sym $otherPerInstallment", 320f, y + 14f, paint)

            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = primaryColor
            canvas.drawText("$sym ${inst.expectedAmount}", 410f, y + 14f, paint)
            paint.typeface = Typeface.DEFAULT

            paint.color = when (inst.status.name) {
                "PAID" -> Color.parseColor("#059669")
                "OVERDUE" -> Color.parseColor("#DC2626")
                else -> Color.parseColor("#475569")
            }
            canvas.drawText(inst.status.displayName, 505f, y + 14f, paint)

            y += 20f
        }

        // Línea inferior de la tabla
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(35f, y, 577f, y, paint)

        // Pie de Página
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Documento emitido por el sistema de $companyDisplayName • Teléfono de contacto: $phone", 35f, 770f, paint)
    }
}
