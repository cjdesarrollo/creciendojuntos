package com.prestamos.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.export.PdfReceiptGenerator
import com.prestamos.app.data.export.ReceiptFormat
import com.prestamos.app.data.hardware.BluetoothPrinterDevice
import com.prestamos.app.data.hardware.BluetoothPrinterManager
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.Installment
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.Payment
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal

data class PaymentUiState(
    val selectedLoan: Loan? = null,
    val selectedClient: Client? = null,
    val installments: List<Installment> = emptyList(),
    val selectedInstallmentId: Long? = null,
    val amountInput: String = "",
    val paymentType: String = "CUOTA", // "CUOTA", "ADELANTO", "CAPITAL"
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val receiptFormat: ReceiptFormat = ReceiptFormat.HALF_LETTER_LANDSCAPE,
    val notes: String = "",
    val isPaymentSuccess: Boolean = false,
    val lastRecordedPayment: Payment? = null,
    val generatedPdfFile: File? = null,
    val shareableContentUri: Uri? = null,
    val pairedPrinters: List<BluetoothPrinterDevice> = emptyList(),
    val isPrinting: Boolean = false,
    val printMessage: String? = null,
    val errorMessage: String? = null
)

class PaymentViewModel(
    private val repository: LoanRepository,
    private val bluetoothPrinterManager: BluetoothPrinterManager,
    private val pdfReceiptGenerator: PdfReceiptGenerator,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    val allPayments: StateFlow<List<Payment>> = repository.getAllPayments(0L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoans: StateFlow<List<Loan>> = repository.getAllLoans(0L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun loadLoanDetails(loanId: Long) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            val client = loan?.let { repository.getClientById(it.clientId) }

            repository.getInstallmentsForLoan(loanId).collect { installments ->
                val nextPending = installments.firstOrNull { it.remainingAmount > BigDecimal.ZERO }
                val defaultAmount = nextPending?.remainingAmount?.toPlainString() ?: loan?.installmentAmount?.toPlainString() ?: ""

                _uiState.value = _uiState.value.copy(
                    selectedLoan = loan,
                    selectedClient = client,
                    installments = installments,
                    amountInput = defaultAmount,
                    errorMessage = null
                )
            }
        }
    }

    fun onAmountInputChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountInput = amount)
    }

    fun onPaymentTypeChanged(type: String) {
        _uiState.value = _uiState.value.copy(paymentType = type)
    }

    fun onPaymentMethodChanged(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun onReceiptFormatChanged(format: ReceiptFormat) {
        _uiState.value = _uiState.value.copy(receiptFormat = format)
        // Recalcular recibo si existe un pago grabado recientemente
        val state = _uiState.value
        val client = state.selectedClient
        val loan = state.selectedLoan
        val lastPayment = state.lastRecordedPayment
        if (client != null && loan != null && lastPayment != null) {
            val pdfFile = pdfReceiptGenerator.generateReceiptPdf(client, loan, lastPayment, format)
            val contentUri = pdfFile?.let { fileStorageManager.getShareableContentUri(it) }
            _uiState.value = state.copy(generatedPdfFile = pdfFile, shareableContentUri = contentUri)
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onSelectInstallment(installment: Installment) {
        val currentSelected = _uiState.value.selectedInstallmentId
        if (currentSelected == installment.id) {
            // Deseleccionar si se toca la misma
            clearSelectedInstallment()
        } else {
            val amountToPay = if (installment.remainingAmount > BigDecimal.ZERO) {
                installment.remainingAmount
            } else {
                installment.expectedAmount
            }
            _uiState.value = _uiState.value.copy(
                selectedInstallmentId = installment.id,
                amountInput = amountToPay.toPlainString()
            )
        }
    }

    fun clearSelectedInstallment() {
        val nextPending = _uiState.value.installments.firstOrNull { it.remainingAmount > BigDecimal.ZERO }
        val defaultAmount = nextPending?.remainingAmount?.toPlainString() ?: _uiState.value.selectedLoan?.installmentAmount?.toPlainString() ?: ""
        _uiState.value = _uiState.value.copy(
            selectedInstallmentId = null,
            amountInput = defaultAmount
        )
    }

    /**
     * Ejecuta el cobro de forma atómica en Room.
     */
    fun recordPayment() {
        val state = _uiState.value
        val loan = state.selectedLoan
        val client = state.selectedClient
        val amountBD = state.amountInput.toBigDecimalOrNull()

        if (loan == null || client == null) {
            _uiState.value = state.copy(errorMessage = "Préstamo no seleccionado")
            return
        }

        if (amountBD == null || amountBD <= BigDecimal.ZERO) {
            _uiState.value = state.copy(errorMessage = "Ingresa un monto de cobro válido")
            return
        }

        viewModelScope.launch {
            try {
                val payment = Payment(
                    loanId = loan.id,
                    installmentId = state.selectedInstallmentId,
                    companyId = loan.companyId,
                    amountPaid = amountBD,
                    paymentType = state.paymentType,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = state.paymentMethod,
                    notes = state.notes.ifBlank { null }
                )

                val paymentId = repository.recordPaymentAtomic(payment)
                val recordedPayment = payment.copy(id = paymentId)

                // Generar Recibo PDF con el formato elegido
                val pdfFile = pdfReceiptGenerator.generateReceiptPdf(client, loan, recordedPayment, state.receiptFormat)
                val contentUri = pdfFile?.let { fileStorageManager.getShareableContentUri(it) }

                // Obtener impresoras emparejadas
                val printers = bluetoothPrinterManager.getPairedPrinters()

                _uiState.value = state.copy(
                    isPaymentSuccess = true,
                    lastRecordedPayment = recordedPayment,
                    generatedPdfFile = pdfFile,
                    shareableContentUri = contentUri,
                    pairedPrinters = printers,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(errorMessage = e.localizedMessage ?: "Error al registrar el cobro")
            }
        }
    }

    fun printReceiptToBluetoothDevice(printerAddress: String) {
        val state = _uiState.value
        val client = state.selectedClient
        val loan = state.selectedLoan
        val payment = state.lastRecordedPayment ?: return

        if (client == null || loan == null) return

        viewModelScope.launch {
            _uiState.value = state.copy(isPrinting = true, printMessage = "Conectando e imprimiendo...")
            val result = bluetoothPrinterManager.printReceipt(printerAddress, client, loan, payment)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printMessage = "¡Impresión exitosa!")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(printMessage = "Error al imprimir: ${e.localizedMessage}")
                }
            )
            _uiState.value = _uiState.value.copy(isPrinting = false)
        }
    }

    fun cancelTotalLoanDebt(loanId: Long, onComplete: () -> Unit = {}) {
        val state = _uiState.value
        val loan = state.selectedLoan ?: return
        val remaining = loan.remainingBalance
        if (remaining <= BigDecimal.ZERO) return

        viewModelScope.launch {
            try {
                val payment = Payment(
                    loanId = loan.id,
                    companyId = loan.companyId,
                    amountPaid = remaining,
                    paymentType = "CANCELACION_TOTAL",
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = PaymentMethod.CASH,
                    notes = "Cancelación de totalidad del crédito"
                )
                repository.recordPaymentAtomic(payment)
                loadLoanDetails(loanId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun recordEarlySettlementPayment(
        loanId: Long,
        quote: com.prestamos.app.domain.calculator.EarlySettlementQuote,
        onComplete: () -> Unit = {}
    ) {
        val state = _uiState.value
        val loan = state.selectedLoan ?: return
        if (quote.totalToPay <= BigDecimal.ZERO) return

        viewModelScope.launch {
            try {
                val payment = Payment(
                    loanId = loan.id,
                    companyId = loan.companyId,
                    amountPaid = quote.totalToPay,
                    paymentType = "CANCELACION_ANTICIPADA",
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = PaymentMethod.CASH,
                    notes = "Cancelación anticipada: Capital C$ ${quote.remainingPrincipal} + Intereses devengados C$ ${quote.accruedInterest}"
                )
                repository.recordPaymentAtomic(payment)
                loadLoanDetails(loanId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun updateLoanDetails(
        loanId: Long,
        companyId: Long,
        principal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        startDateMillis: Long,
        scheduledDate: Long,
        capitalSources: Map<Long, BigDecimal> = emptyMap(),
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateLoanDetails(
                    loanId = loanId,
                    companyId = companyId,
                    principal = principal,
                    interestPercent = interestPercent,
                    totalInstallments = totalInstallments,
                    frequency = frequency,
                    startDateMillis = startDateMillis,
                    scheduledDisbursementDate = scheduledDate,
                    capitalSources = capitalSources
                )
                loadLoanDetails(loanId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun resetPaymentState() {
        _uiState.value = PaymentUiState()
    }

    fun refinanceLoan(
        oldLoanId: Long,
        companyId: Long,
        clientId: Long,
        currency: CurrencyType,
        newPrincipal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        additionalDisbursement: BigDecimal,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val newLoanId = repository.refinanceLoan(
                    oldLoanId = oldLoanId,
                    companyId = companyId,
                    clientId = clientId,
                    currency = currency,
                    newPrincipal = newPrincipal,
                    interestPercent = interestPercent,
                    totalInstallments = totalInstallments,
                    frequency = frequency,
                    additionalDisbursement = additionalDisbursement
                )
                loadLoanDetails(newLoanId)
                onComplete(newLoanId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }
}
