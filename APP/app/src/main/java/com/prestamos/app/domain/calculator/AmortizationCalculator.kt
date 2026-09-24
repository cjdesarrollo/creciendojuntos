package com.prestamos.app.domain.calculator

import com.prestamos.app.domain.model.AmortizationSchedule
import com.prestamos.app.domain.model.Installment
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.PaymentFrequency
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

object AmortizationCalculator {

    /**
     * Calcula la tabla de amortización usando Interés Fijo Mensual sobre Capital Inicial.
     * La tasa (% interestRatePercent) es una Tasa Mensual Fija.
     *
     * 1. Abono a Capital por Cuota = Capital / Total Cuotas
     * 2. Interés Mensual Base = Capital * (% / 100)
     * 3. Interés por Cuota según Frecuencia:
     *    - Mensual (1 cuota/mes)   : Interés Mensual Base / 1
     *    - Quincenal (2 cuotas/mes): Interés Mensual Base / 2
     *    - Semanal (4 cuotas/mes)  : Interés Mensual Base / 4
     *    - Diario (30 cuotas/mes)  : Interés Mensual Base / 30
     * 4. Cuota Total = Abono a Capital por Cuota + Interés por Cuota
     * 5. Interés Total Préstamo = Interés por Cuota * Total Cuotas
     * 6. Monto Total a Pagar = Capital + Interés Total Préstamo
     */
    fun calculateSchedule(
        principal: BigDecimal,
        interestRatePercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        startDateMillis: Long = System.currentTimeMillis(),
        currency: com.prestamos.app.domain.model.CurrencyType = com.prestamos.app.domain.model.CurrencyType.NIO
    ): AmortizationSchedule {
        require(totalInstallments > 0) { "El número de cuotas debe ser mayor a 0" }
        require(principal > BigDecimal.ZERO) { "El capital debe ser mayor a 0" }

        val hundred = BigDecimal("100")
        val interestRateDecimal = interestRatePercent.divide(hundred, 6, RoundingMode.HALF_UP)

        // Interés Mensual Base en dinero (sobre capital inicial)
        val monthlyInterestBase = principal.multiply(interestRateDecimal).setScale(2, RoundingMode.HALF_UP)

        // Número de cuotas que entran en 1 mes según la frecuencia seleccionada
        val installmentsPerMonth = when (frequency) {
            PaymentFrequency.DAILY -> BigDecimal("30")
            PaymentFrequency.WEEKLY -> BigDecimal("4")
            PaymentFrequency.BIWEEKLY -> BigDecimal("2")
            PaymentFrequency.MONTHLY -> BigDecimal("1")
        }

        // Interés Fijo por cada Cuota
        val interestPerInstallment = monthlyInterestBase.divide(installmentsPerMonth, 2, RoundingMode.HALF_UP)

        // Abono a Capital Fijo por cada Cuota
        val installmentCountBD = BigDecimal(totalInstallments)
        val capitalPerInstallment = principal.divide(installmentCountBD, 2, RoundingMode.HALF_UP)

        // Otros Gastos: 1 USD o 37 NIO al mes prorrateado en las cuotas del mes
        val extraFeePerMonth = if (currency == com.prestamos.app.domain.model.CurrencyType.USD) BigDecimal("1.00") else BigDecimal("37.00")
        val extraFeePerInstallment = extraFeePerMonth.divide(installmentsPerMonth, 2, RoundingMode.HALF_UP)

        // Monto Total Regular de la Cuota = Abono Capital + Interés por Cuota + Otros Gastos
        val rawInstallmentAmount = capitalPerInstallment.add(interestPerInstallment).add(extraFeePerInstallment).setScale(2, RoundingMode.HALF_UP)

        // Interés Total acumulado del préstamo = Interés por Cuota * Total de Cuotas
        val totalInterest = interestPerInstallment.multiply(installmentCountBD).setScale(2, RoundingMode.HALF_UP)
        // Monto extra total
        val totalExtraFee = extraFeePerInstallment.multiply(installmentCountBD).setScale(2, RoundingMode.HALF_UP)
        val totalAmount = principal.add(totalInterest).add(totalExtraFee).setScale(2, RoundingMode.HALF_UP)

        val installments = mutableListOf<Installment>()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }

        var accumulatedAmount = BigDecimal.ZERO

        for (i in 1..totalInstallments) {
            // Avanzar fecha de vencimiento según la frecuencia seleccionada
            when (frequency) {
                PaymentFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                PaymentFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                PaymentFrequency.BIWEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 15)
                PaymentFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            }

            // Ajuste en la última cuota para evitar descalce de centavos por redondeo
            val expectedAmount = if (i == totalInstallments) {
                totalAmount.subtract(accumulatedAmount).setScale(2, RoundingMode.HALF_UP)
            } else {
                rawInstallmentAmount
            }

            accumulatedAmount = accumulatedAmount.add(expectedAmount)

            installments.add(
                Installment(
                    id = 0,
                    loanId = 0,
                    installmentNumber = i,
                    dueDate = calendar.timeInMillis,
                    expectedAmount = expectedAmount,
                    paidAmount = BigDecimal.ZERO,
                    remainingAmount = expectedAmount,
                    status = InstallmentStatus.PENDING
                )
            )
        }

        return AmortizationSchedule(
            principal = principal,
            interestPercent = interestRatePercent,
            totalInterest = totalInterest,
            totalAmount = totalAmount,
            installmentAmount = rawInstallmentAmount,
            extraFeePerInstallment = extraFeePerInstallment,
            totalExtraFee = totalExtraFee,
            installments = installments
        )
    }

    /**
     * Calcula la diferencia de dinero en el arqueo físico de caja.
     * Diferencia = Efectivo Físico Contado - Efectivo Esperado en Sistema
     */
    fun calculateCashAuditDifference(
        expectedCash: BigDecimal,
        physicalCash: BigDecimal
    ): BigDecimal {
        return physicalCash.subtract(expectedCash).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calcula la Cancelación Anticipada (Liquidación Justa):
     * Cobra ÚNICAMENTE el capital pendiente y los intereses devengados a la fecha actual,
     * exonerando los intereses futuros no causados.
     */
    fun calculateEarlySettlement(
        loan: com.prestamos.app.domain.model.Loan,
        installments: List<Installment>,
        currentDateMillis: Long = System.currentTimeMillis()
    ): EarlySettlementQuote {
        val totalCount = installments.size.coerceAtLeast(1)
        val countBD = BigDecimal(totalCount)

        val capitalPerInst = loan.principalAmount.divide(countBD, 2, RoundingMode.HALF_UP)
        val interestPerInst = loan.totalInterestAmount.divide(countBD, 2, RoundingMode.HALF_UP)
        val otherTotal = loan.totalAmountToPay.subtract(loan.principalAmount).subtract(loan.totalInterestAmount).max(BigDecimal.ZERO)
        val otherPerInst = otherTotal.divide(countBD, 2, RoundingMode.HALF_UP)

        // Cuotas ya pagadas
        val paidCount = installments.count { it.status == InstallmentStatus.PAID }
        val capitalPaid = capitalPerInst.multiply(BigDecimal(paidCount)).min(loan.principalAmount)
        val remainingPrincipal = loan.principalAmount.subtract(capitalPaid).max(BigDecimal.ZERO)

        // Identificar cuotas vencidas y cuota corriente (hasta la fecha de hoy)
        val pendingInstallments = installments.filter { it.status != InstallmentStatus.PAID }
        val dueOrCurrentPending = pendingInstallments.filter { it.dueDate <= currentDateMillis }

        // Si no hay ninguna cuota vencida, se cobra el interés del período en curso (1 cuota de interés)
        val accruedInterestCount = if (dueOrCurrentPending.isEmpty() && pendingInstallments.isNotEmpty()) 1 else dueOrCurrentPending.size
        val accruedInterest = interestPerInst.multiply(BigDecimal(accruedInterestCount)).setScale(2, RoundingMode.HALF_UP)
        val accruedOther = otherPerInst.multiply(BigDecimal(accruedInterestCount)).setScale(2, RoundingMode.HALF_UP)

        val totalToPay = remainingPrincipal.add(accruedInterest).add(accruedOther).setScale(2, RoundingMode.HALF_UP)
        val interestDiscountSaved = loan.remainingBalance.subtract(totalToPay).max(BigDecimal.ZERO)

        return EarlySettlementQuote(
            remainingPrincipal = remainingPrincipal,
            accruedInterest = accruedInterest,
            accruedOtherFees = accruedOther,
            totalToPay = totalToPay,
            interestDiscountSaved = interestDiscountSaved,
            totalOriginalAmount = loan.totalAmountToPay,
            totalAlreadyPaid = loan.totalPaidAmount
        )
    }
}

data class EarlySettlementQuote(
    val remainingPrincipal: BigDecimal,
    val accruedInterest: BigDecimal,
    val accruedOtherFees: BigDecimal,
    val totalToPay: BigDecimal,
    val interestDiscountSaved: BigDecimal,
    val totalOriginalAmount: BigDecimal,
    val totalAlreadyPaid: BigDecimal
)

