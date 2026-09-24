package com.prestamos.app.domain.calculator

import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.PaymentFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AmortizationCalculatorTest {

    @Test
    fun testNioMonthlyInstallments() {
        // En Córdobas (NIO): Otros gastos = C$ 37.00 / mes
        // Capital: 10,000 | 10% mensual | 5 Cuotas Mensuales
        val principal = BigDecimal("10000.00")
        val interestRatePercent = BigDecimal("10.00")
        val totalInstallments = 5
        val frequency = PaymentFrequency.MONTHLY

        val schedule = AmortizationCalculator.calculateSchedule(
            principal = principal,
            interestRatePercent = interestRatePercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            currency = CurrencyType.NIO
        )

        // Abono capital = 2,000 | Interés = 1,000 | Otros gastos = 37 | Cuota total = 3,037
        assertEquals(BigDecimal("10000.00"), schedule.principal)
        assertEquals(BigDecimal("5000.00"), schedule.totalInterest)
        assertEquals(BigDecimal("37.00"), schedule.extraFeePerInstallment)
        assertEquals(BigDecimal("185.00"), schedule.totalExtraFee)
        assertEquals(BigDecimal("15185.00"), schedule.totalAmount)
        assertEquals(BigDecimal("3037.00"), schedule.installmentAmount)
        assertEquals(5, schedule.installments.size)

        // Suma acumulada exactamente 15,185.00
        val totalInstallmentsSum = schedule.installments.fold(BigDecimal.ZERO) { acc, inst ->
            acc.add(inst.expectedAmount)
        }
        assertEquals(BigDecimal("15185.00"), totalInstallmentsSum)
    }

    @Test
    fun testUsdMonthlyInstallments() {
        // En Dólares (USD): Otros gastos = $ 1.00 / mes (NO 37)
        // Capital: 10,000 | 10% mensual | 5 Cuotas Mensuales
        val principal = BigDecimal("10000.00")
        val interestRatePercent = BigDecimal("10.00")
        val totalInstallments = 5
        val frequency = PaymentFrequency.MONTHLY

        val schedule = AmortizationCalculator.calculateSchedule(
            principal = principal,
            interestRatePercent = interestRatePercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            currency = CurrencyType.USD
        )

        // Abono capital = 2,000 | Interés = 1,000 | Otros gastos = 1.00 | Cuota total = 3,001.00
        assertEquals(BigDecimal("10000.00"), schedule.principal)
        assertEquals(BigDecimal("5000.00"), schedule.totalInterest)
        assertEquals(BigDecimal("1.00"), schedule.extraFeePerInstallment)
        assertEquals(BigDecimal("5.00"), schedule.totalExtraFee)
        assertEquals(BigDecimal("15005.00"), schedule.totalAmount)
        assertEquals(BigDecimal("3001.00"), schedule.installmentAmount)
        assertEquals(5, schedule.installments.size)

        val totalInstallmentsSum = schedule.installments.fold(BigDecimal.ZERO) { acc, inst ->
            acc.add(inst.expectedAmount)
        }
        assertEquals(BigDecimal("15005.00"), totalInstallmentsSum)
    }

    @Test
    fun testUsdBiweeklyInstallments() {
        // En Dólares (USD): Otros gastos = $ 1.00 / mes prorrateado en 2 cuotas/mes = $ 0.50 / cuota
        // Capital: 10,000 | 10% mensual | 10 Cuotas Quincenales (5 meses)
        val principal = BigDecimal("10000.00")
        val interestRatePercent = BigDecimal("10.00")
        val totalInstallments = 10
        val frequency = PaymentFrequency.BIWEEKLY

        val schedule = AmortizationCalculator.calculateSchedule(
            principal = principal,
            interestRatePercent = interestRatePercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            currency = CurrencyType.USD
        )

        // Abono capital = 1,000 | Interés quincenal = 500 | Otros gastos = 0.50 | Cuota = 1,500.50
        assertEquals(BigDecimal("10000.00"), schedule.principal)
        assertEquals(BigDecimal("5000.00"), schedule.totalInterest)
        assertEquals(BigDecimal("0.50"), schedule.extraFeePerInstallment)
        assertEquals(BigDecimal("5.00"), schedule.totalExtraFee)
        assertEquals(BigDecimal("15005.00"), schedule.totalAmount)
        assertEquals(BigDecimal("1500.50"), schedule.installmentAmount)
        assertEquals(10, schedule.installments.size)

        val totalInstallmentsSum = schedule.installments.fold(BigDecimal.ZERO) { acc, inst ->
            acc.add(inst.expectedAmount)
        }
        assertEquals(BigDecimal("15005.00"), totalInstallmentsSum)
    }

    @Test
    fun testCashAuditDifferenceCalculation() {
        val expectedCash = BigDecimal("5000.00")
        val physicalCash = BigDecimal("4950.00") // Faltan 50

        val difference = AmortizationCalculator.calculateCashAuditDifference(expectedCash, physicalCash)
        assertEquals(BigDecimal("-50.00"), difference)
    }
}
