package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.prestamos.app.data.local.db.entities.InstallmentEntity
import com.prestamos.app.data.local.db.entities.LoanEntity
import com.prestamos.app.data.local.db.entities.PaymentEntity
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.LoanStatus
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
abstract class PaymentDao {

    @Query("SELECT * FROM pagos ORDER BY fecha_pago DESC")
    abstract fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM pagos WHERE prestamo_id = :loanId ORDER BY fecha_pago DESC")
    abstract fun getPaymentsForLoan(loanId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM pagos WHERE fecha_pago >= :startOfDayMillis AND fecha_pago <= :endOfDayMillis")
    abstract fun getPaymentsForDateRange(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM pagos WHERE fecha_pago >= :startOfDayMillis AND fecha_pago <= :endOfDayMillis")
    abstract suspend fun getPaymentsListForDateRange(startOfDayMillis: Long, endOfDayMillis: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPaymentInternal(payment: PaymentEntity): Long

    @Query("SELECT * FROM prestamos WHERE id = :loanId")
    abstract suspend fun getLoanByIdInternal(loanId: Long): LoanEntity?

    @Query("SELECT * FROM cuotas WHERE prestamo_id = :loanId ORDER BY numero_cuota ASC")
    abstract suspend fun getInstallmentsInternal(loanId: Long): List<InstallmentEntity>

    @androidx.room.Update
    abstract suspend fun updateLoanInternal(loan: LoanEntity)

    @androidx.room.Update
    abstract suspend fun updateInstallmentInternal(installment: InstallmentEntity)

    /**
     * Transacción atómica local: registra el pago, distribuye el dinero entre las cuotas pendientes
     * o aplica abonar a capital y actualiza el saldo total y estado del préstamo.
     */
    @Transaction
    open suspend fun recordPaymentAtomic(payment: PaymentEntity): Long {
        val paymentId = insertPaymentInternal(payment)
        val loan = getLoanByIdInternal(payment.prestamoId)
            ?: throw IllegalStateException("Préstamo no encontrado ID: ${payment.prestamoId}")

        var remainingPayment = payment.montoPagado

        if (payment.tipoPago == "CAPITAL") {
            // Abono a capital directo
            val newTotalPaid = loan.montoTotalPagado.add(payment.montoPagado)
            val isFullyPaid = newTotalPaid >= loan.montoTotalAPagar
            val newStatus = if (isFullyPaid) LoanStatus.PAID else LoanStatus.ACTIVE
            updateLoanInternal(
                loan.copy(
                    montoTotalPagado = newTotalPaid,
                    estado = newStatus
                )
            )
            return paymentId
        }

        // Pago regular de cuotas, con prioridad en la cuota seleccionada si fue especificada
        val installments = getInstallmentsInternal(payment.prestamoId)

        // Si se seleccionó una cuota específica, aplicarla en primer lugar
        if (payment.cuotaId != null) {
            val targetedInstallment = installments.find { it.id == payment.cuotaId }
            if (targetedInstallment != null && remainingPayment > BigDecimal.ZERO) {
                val pendingOnTarget = targetedInstallment.montoEsperado.subtract(targetedInstallment.montoPagado)
                if (pendingOnTarget > BigDecimal.ZERO) {
                    if (remainingPayment >= pendingOnTarget) {
                        updateInstallmentInternal(
                            targetedInstallment.copy(
                                montoPagado = targetedInstallment.montoEsperado,
                                estado = InstallmentStatus.PAID
                            )
                        )
                        remainingPayment = remainingPayment.subtract(pendingOnTarget)
                    } else {
                        updateInstallmentInternal(
                            targetedInstallment.copy(
                                montoPagado = targetedInstallment.montoPagado.add(remainingPayment),
                                estado = InstallmentStatus.PARTIAL
                            )
                        )
                        remainingPayment = BigDecimal.ZERO
                    }
                }
            }
        }

        // Si aún queda dinero restante (o no se especificó cuota), distribuir secuencialmente
        for (installment in installments) {
            if (remainingPayment <= BigDecimal.ZERO) break
            if (installment.id == payment.cuotaId) continue // Ya fue procesada

            val pendingOnInstallment = installment.montoEsperado.subtract(installment.montoPagado)
            if (pendingOnInstallment > BigDecimal.ZERO) {
                if (remainingPayment >= pendingOnInstallment) {
                    // Cuota completada
                    val newPaidAmount = installment.montoEsperado
                    updateInstallmentInternal(
                        installment.copy(
                            montoPagado = newPaidAmount,
                            estado = InstallmentStatus.PAID
                        )
                    )
                    remainingPayment = remainingPayment.subtract(pendingOnInstallment)
                } else {
                    // Pago parcial
                    val newPaidAmount = installment.montoPagado.add(remainingPayment)
                    updateInstallmentInternal(
                        installment.copy(
                            montoPagado = newPaidAmount,
                            estado = InstallmentStatus.PARTIAL
                        )
                    )
                    remainingPayment = BigDecimal.ZERO
                }
            }
        }

        // Actualizar totales del préstamo
        val newTotalPaid = loan.montoTotalPagado.add(payment.montoPagado)
        val isFullyPaid = newTotalPaid >= loan.montoTotalAPagar
        val newStatus = if (isFullyPaid) LoanStatus.PAID else LoanStatus.ACTIVE

        updateLoanInternal(
            loan.copy(
                montoTotalPagado = newTotalPaid,
                estado = newStatus
            )
        )

        return paymentId
    }
}
