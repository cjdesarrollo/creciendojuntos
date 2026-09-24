package com.prestamos.app.domain.model

import androidx.annotation.DrawableRes
import com.prestamos.app.R
import java.math.BigDecimal

enum class Company(
    val id: Long,
    val displayName: String,
    val shortName: String,
    @DrawableRes val iconRes: Int? = null,
    val defaultPhone: String = ""
) {
    ALL(0L, "Todas las Empresas", "Todas", null, ""),
    CRECIENDO_JUNTOS(1L, "Creciendo Juntos", "Creciendo Juntos", R.drawable.logo_cj, "8232 2262"),
    FACILITO(2L, "Facilito", "Facilito", R.drawable.logo_facilito, "8797 3321")
}

enum class CurrencyType(val code: String, val symbol: String, val displayName: String) {
    NIO("NIO", "C$", "Córdobas (C$)"),
    USD("USD", "US$", "Dólares (US$)")
}

enum class PaymentFrequency(val displayName: String, val daysInterval: Int) {
    DAILY("Diario", 1),
    WEEKLY("Semanal", 7),
    BIWEEKLY("Quincenal", 15),
    MONTHLY("Mensual", 30)
}

enum class LoanStatus(val displayName: String) {
    ACTIVE("Activo"),
    PAID("Pagado"),
    OVERDUE("En Mora"),
    CANCELLED("Cancelado")
}

enum class ClientStatus(val displayName: String) {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    OVERDUE("En Mora")
}

enum class InstallmentStatus(val displayName: String) {
    PENDING("Pendiente"),
    PARTIAL("Parcial"),
    PAID("Pagado"),
    OVERDUE("Vencido")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Efectivo"),
    TRANSFER("Transferencia")
}

enum class RiskLevel(val displayName: String) {
    LOW("Bajo"),
    MEDIUM("Medio"),
    HIGH("Alto")
}

enum class CashMovementType(val displayName: String) {
    INCOME("Ingreso"),
    EXPENSE("Egreso"),
    ADJUSTMENT("Ajuste")
}

data class Client(
    val id: Long = 0,
    val fullName: String,
    val dniOrId: String,
    val phone: String,
    val address: String,
    val email: String? = null,
    val notes: String? = null,
    val reference1Name: String? = null,
    val reference1Phone: String? = null,
    val reference1Relation: String? = null,
    val reference2Name: String? = null,
    val reference2Phone: String? = null,
    val reference2Relation: String? = null,
    val estimatedInterestPercent: BigDecimal? = null,
    val idPhotoUri: String? = null,
    val docPhotoUri: String? = null,
    val signatureUri: String? = null,
    val isUncollectible: Boolean = false,
    val isUnderRecovery: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ClientWithCompliance(
    val client: Client,
    val activeLoansCount: Int = 0,
    val activeBalanceNio: BigDecimal = BigDecimal.ZERO,
    val activeBalanceUsd: BigDecimal = BigDecimal.ZERO,
    val totalPaidInstallments: Int = 0,
    val overdueInstallments: Int = 0,
    val compliancePercent: Int = 100,
    val status: ClientStatus = ClientStatus.ACTIVE,
    val loanProgressPercent: Int = 0
)

data class Loan(
    val id: Long = 0,
    val clientId: Long,
    val companyId: Long = 1L,
    val clientName: String = "",
    val currency: CurrencyType = CurrencyType.NIO,
    val principalAmount: BigDecimal,
    val interestRatePercent: BigDecimal,
    val totalInterestAmount: BigDecimal,
    val totalAmountToPay: BigDecimal,
    val paymentFrequency: PaymentFrequency,
    val totalInstallments: Int,
    val installmentAmount: BigDecimal,
    val startDate: Long,
    val dueDate: Long,
    val scheduledDisbursementDate: Long = startDate,
    val isDisbursed: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val totalPaidAmount: BigDecimal = BigDecimal.ZERO,
    val remainingBalance: BigDecimal = totalAmountToPay,
    val createdAt: Long = System.currentTimeMillis()
)

data class Installment(
    val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val dueDate: Long,
    val expectedAmount: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val remainingAmount: BigDecimal = expectedAmount,
    val status: InstallmentStatus = InstallmentStatus.PENDING
)

data class Payment(
    val id: Long = 0,
    val loanId: Long,
    val installmentId: Long? = null,
    val companyId: Long = 1L,
    val amountPaid: BigDecimal,
    val paymentType: String = "CUOTA", // "CUOTA", "ADELANTO", "CAPITAL"
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val receiptUri: String? = null,
    val notes: String? = null
)

data class CashAudit(
    val id: Long = 0,
    val companyId: Long = 1L,
    val currency: CurrencyType = CurrencyType.NIO,
    val auditDate: Long = System.currentTimeMillis(),
    val expectedCash: BigDecimal,
    val actualPhysicalCash: BigDecimal,
    val difference: BigDecimal,
    val notes: String? = null
)

data class CashSession(
    val id: Long = 0,
    val companyId: Long = 1L,
    val currency: CurrencyType = CurrencyType.NIO,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val openDate: Long = System.currentTimeMillis(),
    val closeDate: Long? = null,
    val status: String = "ABIERTA",
    val isAutoOpened: Boolean = false,
    val notes: String? = null
)

data class Disbursement(
    val id: Long = 0,
    val loanId: Long,
    val clientId: Long,
    val clientName: String = "",
    val companyId: Long = 1L,
    val amount: BigDecimal,
    val currency: CurrencyType = CurrencyType.NIO,
    val scheduledDate: Long,
    val actualDisbursementDate: Long? = null,
    val method: PaymentMethod = PaymentMethod.CASH,
    val status: String = "PROGRAMADO",
    val receiptUri: String? = null,
    val notes: String? = null
)

data class CashMovement(
    val id: Long = 0,
    val companyId: Long = 1L,
    val currency: CurrencyType = CurrencyType.NIO,
    val type: CashMovementType = CashMovementType.INCOME,
    val amount: BigDecimal,
    val concept: String,
    val category: String? = null,
    val loanId: Long? = null,
    val date: Long = System.currentTimeMillis()
)

data class LoanRequest(
    val loanId: Long,
    val clientName: String,
    val requestedAmount: BigDecimal,
    val currency: CurrencyType = CurrencyType.NIO,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val companyId: Long = 1L
)

data class UpcomingPayment(
    val loanId: Long,
    val clientName: String,
    val dueDate: Long,
    val amount: BigDecimal,
    val currency: CurrencyType = CurrencyType.NIO,
    val companyId: Long = 1L
)

data class DashboardSummary(
    val activeLoansCount: Int = 0,
    val totalLoansCount: Int = 0,
    val totalPortfolioNio: BigDecimal = BigDecimal.ZERO,
    val totalPortfolioUsd: BigDecimal = BigDecimal.ZERO,
    val pendingBalanceNio: BigDecimal = BigDecimal.ZERO,
    val pendingBalanceUsd: BigDecimal = BigDecimal.ZERO,
    val monthlyIncomeNio: BigDecimal = BigDecimal.ZERO,
    val monthlyIncomeUsd: BigDecimal = BigDecimal.ZERO,
    val newRequestsCount: Int = 0,
    val totalCollectedTodayNio: BigDecimal = BigDecimal.ZERO,
    val totalCollectedTodayUsd: BigDecimal = BigDecimal.ZERO,
    val overdueLoansCount: Int = 0,
    val totalOverdueAmountNio: BigDecimal = BigDecimal.ZERO,
    val totalOverdueAmountUsd: BigDecimal = BigDecimal.ZERO,
    val onTimePercent: Int = 65,
    val delayedPercent: Int = 15,
    val highRiskPercent: Int = 10,
    val cancelledPercent: Int = 10,
    val recentRequests: List<LoanRequest> = emptyList(),
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    val dueTodayCount: Int = 0,
    val dueTodayAmountNio: BigDecimal = BigDecimal.ZERO,
    val dueTodayAmountUsd: BigDecimal = BigDecimal.ZERO,
    val dueThisWeekCount: Int = 0,
    val dueThisWeekAmountNio: BigDecimal = BigDecimal.ZERO,
    val dueThisWeekAmountUsd: BigDecimal = BigDecimal.ZERO,
    val dueThisMonthCount: Int = 0,
    val dueThisMonthAmountNio: BigDecimal = BigDecimal.ZERO,
    val dueThisMonthAmountUsd: BigDecimal = BigDecimal.ZERO
)

data class AmortizationSchedule(
    val principal: BigDecimal,
    val interestPercent: BigDecimal,
    val totalInterest: BigDecimal,
    val totalAmount: BigDecimal,
    val installmentAmount: BigDecimal,
    val extraFeePerInstallment: BigDecimal = BigDecimal.ZERO,
    val totalExtraFee: BigDecimal = BigDecimal.ZERO,
    val installments: List<Installment>
)

data class CashDenominationCount(
    val bill1000: Int = 0,
    val bill500: Int = 0,
    val bill200: Int = 0,
    val bill100: Int = 0,
    val bill50: Int = 0,
    val bill20: Int = 0,
    val bill10: Int = 0,
    val coins: BigDecimal = BigDecimal.ZERO
) {
    fun calculateTotal(currency: CurrencyType): BigDecimal {
        return if (currency == CurrencyType.NIO) {
            val b1000 = BigDecimal(bill1000).multiply(BigDecimal("1000"))
            val b500 = BigDecimal(bill500).multiply(BigDecimal("500"))
            val b200 = BigDecimal(bill200).multiply(BigDecimal("200"))
            val b100 = BigDecimal(bill100).multiply(BigDecimal("100"))
            val b50 = BigDecimal(bill50).multiply(BigDecimal("50"))
            val b20 = BigDecimal(bill20).multiply(BigDecimal("20"))
            val b10 = BigDecimal(bill10).multiply(BigDecimal("10"))
            b1000.add(b500).add(b200).add(b100).add(b50).add(b20).add(b10).add(coins)
        } else {
            val b100 = BigDecimal(bill1000).multiply(BigDecimal("100"))
            val b50 = BigDecimal(bill500).multiply(BigDecimal("50"))
            val b20 = BigDecimal(bill200).multiply(BigDecimal("20"))
            val b10 = BigDecimal(bill100).multiply(BigDecimal("10"))
            val b5 = BigDecimal(bill50).multiply(BigDecimal("5"))
            val b1 = BigDecimal(bill20).multiply(BigDecimal("1"))
            b100.add(b50).add(b20).add(b10).add(b5).add(b1).add(coins)
        }
    }
}

data class Guarantee(
    val id: Long = 0,
    val loanId: Long = 0,
    val type: String,
    val estimatedValue: BigDecimal,
    val state: String,
    val description: String,
    val mainPhotoUri: String? = null,
    val circulationPhotoUri: String? = null
)

data class CapitalOrigin(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val companyId: Long = 0L,
    val isActive: Boolean = true,
    val totalInvested: BigDecimal = BigDecimal.ZERO,
    val totalReturned: BigDecimal = BigDecimal.ZERO,
    val createdAt: Long = System.currentTimeMillis()
)

data class CapitalTransaction(
    val id: Long = 0,
    val originId: Long,
    val companyId: Long? = null,
    val type: String, // "INGRESO", "EGRESO"
    val amount: BigDecimal,
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null
)

data class LoanCapitalSource(
    val id: Long = 0,
    val loanId: Long,
    val originId: Long,
    val amountContributed: BigDecimal
)

data class AppUser(
    val id: Long = 0,
    val fullName: String,
    val username: String,
    val phone: String = "",
    val role: String = "Administrador", // "Administrador", "Cobrador", "Supervisor"
    val pin: String = "1234",
    val photoUri: String? = null,
    val biometricEnabled: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class CompanyProfile(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String = "Nicaragua",
    val ruc: String = "",
    val email: String = "",
    val defaultInterestRate: BigDecimal = BigDecimal("10.00")
)

