package com.prestamos.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.calculator.AmortizationCalculator
import com.prestamos.app.domain.model.AmortizationSchedule
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.DashboardSummary
import com.prestamos.app.domain.model.Installment
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.domain.model.RiskLevel
import com.prestamos.app.domain.model.Guarantee
import com.prestamos.app.domain.model.CapitalOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CreateLoanFormState(
    val selectedClientId: Long? = null,
    val companyId: Long = 1L,
    val currency: CurrencyType = CurrencyType.NIO,
    val principalInput: String = "10000",
    val interestInput: String = "10",
    val totalInstallmentsInput: String = "5",
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val scheduledDisbursementDate: Long = System.currentTimeMillis(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val schedulePreview: AmortizationSchedule? = null,
    val isCreatedSuccess: Boolean = false,
    val createdLoanId: Long? = null,
    val errorMessage: String? = null,
    val guarantees: List<Guarantee> = emptyList(),
    val capitalSources: Map<Long, BigDecimal> = emptyMap() // Map of CapitalOrigin ID to Amount
)

class LoanViewModel(val repository: LoanRepository) : ViewModel() {

    private val _selectedCompanyId = MutableStateFlow(0L) // 0L = Todas, 1L = Creciendo Juntos, 2L = Facilito
    val selectedCompanyId: StateFlow<Long> = _selectedCompanyId.asStateFlow()

    val loans: StateFlow<List<Loan>> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getAllLoans(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getDashboardSummary(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val capitalOrigins: StateFlow<List<CapitalOrigin>> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getAllCapitalOrigins(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _createFormState = MutableStateFlow(CreateLoanFormState())
    val createFormState: StateFlow<CreateLoanFormState> = _createFormState.asStateFlow()

    init {
        recalculateSchedulePreview()
    }

    fun getInstallmentsForLoan(loanId: Long): Flow<List<Installment>> {
        return repository.getInstallmentsForLoan(loanId)
    }

    suspend fun getCapitalSourcesForLoan(loanId: Long): List<com.prestamos.app.domain.model.LoanCapitalSource> {
        return repository.getCapitalSourcesForLoan(loanId)
    }

    fun onCompanyFilterChanged(companyId: Long) {
        _selectedCompanyId.value = companyId
    }

    fun onClientSelected(clientId: Long) {
        _createFormState.value = _createFormState.value.copy(selectedClientId = clientId)
    }

    fun onCompanySelectedInForm(companyId: Long) {
        _createFormState.value = _createFormState.value.copy(companyId = companyId)
    }

    fun onCurrencySelected(currency: CurrencyType) {
        _createFormState.value = _createFormState.value.copy(currency = currency)
        recalculateSchedulePreview()
    }

    fun onRiskLevelSelected(riskLevel: RiskLevel) {
        _createFormState.value = _createFormState.value.copy(riskLevel = riskLevel)
    }

    fun onFormInputChanged(
        principal: String = _createFormState.value.principalInput,
        interest: String = _createFormState.value.interestInput,
        installments: String = _createFormState.value.totalInstallmentsInput,
        frequency: PaymentFrequency = _createFormState.value.paymentFrequency,
        scheduledDate: Long = _createFormState.value.scheduledDisbursementDate
    ) {
        _createFormState.value = _createFormState.value.copy(
            principalInput = principal,
            interestInput = interest,
            totalInstallmentsInput = installments,
            paymentFrequency = frequency,
            scheduledDisbursementDate = scheduledDate
        )
        recalculateSchedulePreview()
    }

    fun addGuarantee(guarantee: Guarantee) {
        val currentGuarantees = _createFormState.value.guarantees.toMutableList()
        currentGuarantees.add(guarantee)
        _createFormState.value = _createFormState.value.copy(guarantees = currentGuarantees)
    }

    fun removeGuarantee(index: Int) {
        val currentGuarantees = _createFormState.value.guarantees.toMutableList()
        if (index in currentGuarantees.indices) {
            currentGuarantees.removeAt(index)
            _createFormState.value = _createFormState.value.copy(guarantees = currentGuarantees)
        }
    }

    fun updateCapitalSource(originId: Long, amount: BigDecimal) {
        val currentSources = _createFormState.value.capitalSources.toMutableMap()
        if (amount > BigDecimal.ZERO) {
            currentSources[originId] = amount
        } else {
            currentSources.remove(originId)
        }
        _createFormState.value = _createFormState.value.copy(capitalSources = currentSources)
    }

    private fun recalculateSchedulePreview() {
        val state = _createFormState.value
        val principal = state.principalInput.toBigDecimalOrNull()
        val interest = state.interestInput.toBigDecimalOrNull()
        val installments = state.totalInstallmentsInput.toIntOrNull()

        if (principal != null && principal > BigDecimal.ZERO &&
            interest != null && interest >= BigDecimal.ZERO &&
            installments != null && installments > 0
        ) {
            val schedule = AmortizationCalculator.calculateSchedule(
                principal = principal,
                interestRatePercent = interest,
                totalInstallments = installments,
                frequency = state.paymentFrequency,
                startDateMillis = state.scheduledDisbursementDate,
                currency = state.currency
            )
            _createFormState.value = _createFormState.value.copy(schedulePreview = schedule, errorMessage = null)
        } else {
            _createFormState.value = _createFormState.value.copy(schedulePreview = null)
        }
    }

    fun createLoan(onSuccess: (Long, Client?, Loan, List<Installment>) -> Unit = { _, _, _, _ -> }) {
        val state = _createFormState.value
        val clientId = state.selectedClientId
        if (clientId == null) {
            _createFormState.value = state.copy(errorMessage = "Debes seleccionar un cliente")
            return
        }

        val schedule = state.schedulePreview
        if (schedule == null) {
            _createFormState.value = state.copy(errorMessage = "Verifica los datos del préstamo")
            return
        }

        viewModelScope.launch {
            try {
                val client = repository.getClientById(clientId)
                if (client?.isUncollectible == true) {
                    _createFormState.value = state.copy(errorMessage = "El cliente está marcado como incobrable. No está permitido otorgarle préstamos.")
                    return@launch
                }

                // Verificar que el total de capital sources coincida con el principal si hay capital sources
                if (state.capitalSources.isNotEmpty()) {
                    val totalSources = state.capitalSources.values.fold(BigDecimal.ZERO) { acc, curr -> acc.add(curr) }
                    if (totalSources.compareTo(schedule.principal) != 0) {
                        _createFormState.value = state.copy(errorMessage = "La suma de los orígenes de capital no coincide con el monto principal a prestar")
                        return@launch
                    }
                }

                val loanId = repository.createLoanWithSchedule(
                    clientId = clientId,
                    companyId = state.companyId,
                    currency = state.currency,
                    principal = schedule.principal,
                    interestPercent = schedule.interestPercent,
                    totalInstallments = schedule.installments.size,
                    frequency = state.paymentFrequency,
                    scheduledDisbursementDate = state.scheduledDisbursementDate,
                    riskLevel = state.riskLevel,
                    guarantees = state.guarantees,
                    capitalSources = state.capitalSources
                )

                val createdLoan = repository.getLoanById(loanId)

                if (createdLoan != null) {
                    val createdInstallments = repository.getInstallmentsForLoan(loanId).first()
                    _createFormState.value = state.copy(
                        isCreatedSuccess = true,
                        createdLoanId = loanId
                    )
                    onSuccess(loanId, client, createdLoan, createdInstallments)
                } else {
                    _createFormState.value = state.copy(
                        isCreatedSuccess = true,
                        createdLoanId = loanId
                    )
                }
            } catch (e: Exception) {
                _createFormState.value = state.copy(errorMessage = e.localizedMessage ?: "Error al crear préstamo")
            }
        }
    }

    fun updateLoan(
        loanId: Long,
        companyId: Long,
        principal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        startDateMillis: Long,
        scheduledDate: Long,
        capitalSources: Map<Long, BigDecimal>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.updateLoanDetails(
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
                onComplete(success)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun resetCreateForm() {
        _createFormState.value = CreateLoanFormState()
        recalculateSchedulePreview()
    }
}
