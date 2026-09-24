package com.prestamos.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.domain.calculator.AmortizationCalculator
import com.prestamos.app.domain.model.CashAudit
import com.prestamos.app.domain.model.CashDenominationCount
import com.prestamos.app.domain.model.CashMovement
import com.prestamos.app.domain.model.CashMovementType
import com.prestamos.app.domain.model.CashSession
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.Disbursement
import com.prestamos.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

data class CashAuditUiState(
    val selectedCurrency: CurrencyType = CurrencyType.NIO,
    val expectedSystemCash: BigDecimal = BigDecimal.ZERO,
    val denominationCount: CashDenominationCount = CashDenominationCount(),
    val manualPhysicalCashInput: String = "",
    val useDenominationCounter: Boolean = true,
    val calculatedPhysicalTotal: BigDecimal = BigDecimal.ZERO,
    val calculatedDifference: BigDecimal = BigDecimal.ZERO,
    val dailyIncomeTotal: BigDecimal = BigDecimal.ZERO,
    val dailyExpenseTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyIncomeTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyExpenseTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyNetBalance: BigDecimal = BigDecimal.ZERO,
    val monthlyMovementCount: Int = 0,
    val notes: String = "",
    val activeCashSession: CashSession? = null,
    val initialBalanceInput: String = "",
    val expenseAmountInput: String = "",
    val expenseConceptInput: String = "",
    val expenseCategoryInput: String = "Gasto de Caja",
    val isExpenseDialogOpen: Boolean = false,
    val isAuditDialogOpen: Boolean = false,
    val isIncomePickerOpen: Boolean = false,
    val weeklyDisbursements: List<Disbursement> = emptyList(),
    val movements: List<CashMovement> = emptyList(),
    val auditHistory: List<CashAudit> = emptyList(),
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CashAuditViewModel(private val repository: LoanRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CashAuditUiState())
    val uiState: StateFlow<CashAuditUiState> = _uiState.asStateFlow()

    private val _selectedCompanyId = MutableStateFlow(0L) // 0L = Todas
    val selectedCompanyId = _selectedCompanyId.asStateFlow()

    val activeSession: StateFlow<CashSession?> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getActiveCashSession(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyDisbursements: StateFlow<List<Disbursement>> = repository.getWeeklyScheduledDisbursements() // No company filter yet for disbursements, optional. We can just leave it as is or add it later.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movements: StateFlow<List<CashMovement>> = _selectedCompanyId
        .flatMapLatest { companyId -> repository.getAllCashMovements(companyId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect companyId changes and reload expected cash
        viewModelScope.launch {
            _selectedCompanyId.collect {
                loadExpectedCashAndHistory()
            }
        }
    }

    fun onCompanyFilterChanged(companyId: Long) {
        _selectedCompanyId.value = companyId
    }

    fun onCurrencyChanged(currency: CurrencyType) {
        _uiState.value = _uiState.value.copy(
            selectedCurrency = currency,
            denominationCount = CashDenominationCount(),
            manualPhysicalCashInput = ""
        )
        loadExpectedCashAndHistory()
    }

    private fun loadExpectedCashAndHistory() {
        viewModelScope.launch {
            val companyId = _selectedCompanyId.value
            combine(
                repository.getDashboardSummary(companyId),
                repository.getAllCashAudits(companyId),
                repository.getAllCashMovements(companyId)
            ) { summary, audits, movs ->
                val currentCurrency = _uiState.value.selectedCurrency
                val expected = if (currentCurrency == CurrencyType.USD) summary.totalCollectedTodayUsd else summary.totalCollectedTodayNio
                val physicalTotal = calculateCurrentPhysicalTotal(currentCurrency)
                val diff = AmortizationCalculator.calculateCashAuditDifference(expected, physicalTotal)

                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH)
                val currentDay = now.get(Calendar.DAY_OF_YEAR)

                fun isToday(ts: Long): Boolean {
                    val cal = Calendar.getInstance().apply { timeInMillis = ts }
                    return cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.DAY_OF_YEAR) == currentDay
                }

                fun isThisMonth(ts: Long): Boolean {
                    val cal = Calendar.getInstance().apply { timeInMillis = ts }
                    return cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                }

                val dailyIncome = movs.filter { isToday(it.date) && it.type == CashMovementType.INCOME }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }
                val dailyExpense = movs.filter { isToday(it.date) && it.type == CashMovementType.EXPENSE }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }

                val monthlyIncome = movs.filter { isThisMonth(it.date) && it.type == CashMovementType.INCOME }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }
                val monthlyExpense = movs.filter { isThisMonth(it.date) && it.type == CashMovementType.EXPENSE }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.amount) }
                val monthlyNet = monthlyIncome.subtract(monthlyExpense)
                val monthlyCount = movs.count { isThisMonth(it.date) }

                _uiState.value.copy(
                    expectedSystemCash = expected,
                    calculatedPhysicalTotal = physicalTotal,
                    calculatedDifference = diff,
                    dailyIncomeTotal = dailyIncome,
                    dailyExpenseTotal = dailyExpense,
                    monthlyIncomeTotal = monthlyIncome,
                    monthlyExpenseTotal = monthlyExpense,
                    monthlyNetBalance = monthlyNet,
                    monthlyMovementCount = monthlyCount,
                    movements = movs,
                    auditHistory = audits.filter { it.currency == currentCurrency }
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onInitialBalanceInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(initialBalanceInput = input)
    }

    fun openCashSessionManual() {
        val input = _uiState.value.initialBalanceInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        viewModelScope.launch {
            try {
                repository.openCashSessionManual(
                    companyId = if (_selectedCompanyId.value == 0L) 1L else _selectedCompanyId.value, // Default to CJ if 'Todas'
                    currency = _uiState.value.selectedCurrency,
                    initialBalance = input,
                    notes = "Apertura manual de caja"
                )
                _uiState.value = _uiState.value.copy(initialBalanceInput = "", errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    // Modal de Registro de Egreso
    fun openExpenseDialog() {
        _uiState.value = _uiState.value.copy(
            isExpenseDialogOpen = true,
            expenseAmountInput = "",
            expenseConceptInput = "",
            errorMessage = null
        )
    }

    fun closeExpenseDialog() {
        _uiState.value = _uiState.value.copy(isExpenseDialogOpen = false, errorMessage = null)
    }

    fun onExpenseAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(expenseAmountInput = amount)
    }

    fun onExpenseConceptChanged(concept: String) {
        _uiState.value = _uiState.value.copy(expenseConceptInput = concept)
    }

    fun registerExpense() {
        val state = _uiState.value
        val amount = state.expenseAmountInput.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.value = state.copy(errorMessage = "Ingresa un monto válido para el egreso")
            return
        }

        if (state.expenseConceptInput.isBlank()) {
            _uiState.value = state.copy(errorMessage = "El concepto de egreso es OBLIGATORIO")
            return
        }

        viewModelScope.launch {
            try {
                repository.registerExpense(
                    amount = amount,
                    concept = state.expenseConceptInput.trim(),
                    category = state.expenseCategoryInput,
                    companyId = if (_selectedCompanyId.value == 0L) 1L else _selectedCompanyId.value
                )
                _uiState.value = state.copy(isExpenseDialogOpen = false, errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = state.copy(errorMessage = e.localizedMessage ?: "Error al registrar egreso")
            }
        }
    }

    fun processDisbursement(disbursementId: Long, method: PaymentMethod, notes: String?) {
        viewModelScope.launch {
            try {
                repository.processDisbursement(disbursementId, method, notes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun onDenominationChanged(count: CashDenominationCount) {
        _uiState.value = _uiState.value.copy(denominationCount = count)
        recalculateTotals()
    }

    fun onManualPhysicalInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(manualPhysicalCashInput = input)
        recalculateTotals()
    }

    fun toggleCounterMode(useDenomination: Boolean) {
        _uiState.value = _uiState.value.copy(useDenominationCounter = useDenomination)
        recalculateTotals()
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    private fun recalculateTotals() {
        val state = _uiState.value
        val physicalTotal = calculateCurrentPhysicalTotal(state.selectedCurrency)
        val difference = AmortizationCalculator.calculateCashAuditDifference(state.expectedSystemCash, physicalTotal)

        _uiState.value = state.copy(
            calculatedPhysicalTotal = physicalTotal,
            calculatedDifference = difference
        )
    }

    private fun calculateCurrentPhysicalTotal(currency: CurrencyType = _uiState.value.selectedCurrency): BigDecimal {
        val state = _uiState.value
        return if (state.useDenominationCounter) {
            state.denominationCount.calculateTotal(currency)
        } else {
            state.manualPhysicalCashInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    fun performAudit() {
        val state = _uiState.value
        val physicalTotal = state.calculatedPhysicalTotal

        viewModelScope.launch {
            try {
                repository.performCashAudit(
                    companyId = if (_selectedCompanyId.value == 0L) 1L else _selectedCompanyId.value,
                    currency = state.selectedCurrency,
                    expectedCash = state.expectedSystemCash,
                    physicalCash = physicalTotal,
                    notes = state.notes.ifBlank { null }
                )
                _uiState.value = state.copy(isSavedSuccess = true, errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = state.copy(errorMessage = e.localizedMessage ?: "Error al guardar el arqueo")
            }
        }
    }

    fun resetState() {
        _uiState.value = CashAuditUiState(expectedSystemCash = _uiState.value.expectedSystemCash)
        recalculateTotals()
    }
}
