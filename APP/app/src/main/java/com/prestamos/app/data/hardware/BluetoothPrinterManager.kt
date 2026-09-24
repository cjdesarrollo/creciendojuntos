package com.prestamos.app.data.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String
)

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothPrinterDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices.map { device ->
            BluetoothPrinterDevice(
                name = device.name ?: "Impresora Térmica",
                address = device.address
            )
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        deviceAddress: String,
        client: Client,
        loan: Loan,
        payment: Payment
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth no disponible"))
        var socket: BluetoothSocket? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            adapter.cancelDiscovery()
            socket.connect()

            val outputStream: OutputStream = socket.outputStream

            // Formatear comandos ESC/POS para la impresora térmica
            val receiptBytes = generateEscPosReceiptBytes(client, loan, payment)
            outputStream.write(receiptBytes)
            outputStream.flush()

            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            try { socket?.close() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private fun generateEscPosReceiptBytes(client: Client, loan: Loan, payment: Payment): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val sym = loan.currency.symbol
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Comandos ESC/POS estándar
        val initPrinter = byteArrayOf(0x1B, 0x40) // ESC @ Reset
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1 (Centro)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0 (Izquierda)
        val fontBoldOn = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1 (Bold)
        val fontBoldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val fontDoubleSize = byteArrayOf(0x1D, 0x21, 0x11) // Double height & width
        val fontNormal = byteArrayOf(0x1D, 0x21, 0x00)
        val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0

        baos.write(initPrinter)
        
        // Encabezado
        baos.write(alignCenter)
        baos.write(fontDoubleSize)
        baos.write(fontBoldOn)
        baos.write("PRESTAMO FACIL\n".toByteArray(Charsets.ISO_8859_1))
        baos.write(fontNormal)
        baos.write(fontBoldOff)
        baos.write("RECIBO DE COBRO OFICIAL\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("--------------------------------\n".toByteArray())

        // Detalles del Pago
        baos.write(alignLeft)
        baos.write("Fecha: ${dateFormat.format(Date(payment.paymentDate))}\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("Recibo Nro: #${payment.id}\n".toByteArray())
        baos.write("Cliente: ${client.fullName}\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("ID/DNI: ${client.dniOrId}\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("Prestamo ID: #${loan.id} (${loan.currency.code})\n".toByteArray())
        baos.write("--------------------------------\n".toByteArray())

        // Montos
        baos.write(fontBoldOn)
        baos.write("MONTO PAGADO: $sym ${payment.amountPaid}\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("Metodo: ${payment.paymentMethod.displayName}\n".toByteArray(Charsets.ISO_8859_1))
        baos.write(fontBoldOff)
        baos.write("--------------------------------\n".toByteArray())

        // Balance de Préstamo
        val totalToPay = loan.totalAmountToPay
        val totalPaid = loan.totalPaidAmount
        val remaining = totalToPay.subtract(totalPaid).max(java.math.BigDecimal.ZERO)

        baos.write("Monto Total Préstamo: $sym $totalToPay\n".toByteArray(Charsets.ISO_8859_1))
        baos.write("Total Cobrado: $sym $totalPaid\n".toByteArray(Charsets.ISO_8859_1))
        baos.write(fontBoldOn)
        baos.write("Saldo Pendiente: $sym $remaining\n".toByteArray(Charsets.ISO_8859_1))

        baos.write(fontBoldOff)
        baos.write("--------------------------------\n".toByteArray())

        // Mensaje de Pie
        baos.write(alignCenter)
        baos.write("¡Gracias por su pago puntual!\n\n\n\n".toByteArray(Charsets.ISO_8859_1))
        baos.write(feedAndCut)

        return baos.toByteArray()
    }
}
