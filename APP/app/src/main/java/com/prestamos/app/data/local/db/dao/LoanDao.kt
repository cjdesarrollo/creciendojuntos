package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.InstallmentEntity
import com.prestamos.app.data.local.db.entities.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM prestamos ORDER BY fecha_creacion DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM prestamos WHERE cliente_id = :clientId ORDER BY fecha_creacion DESC")
    fun getLoansForClient(clientId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM prestamos WHERE id = :loanId")
    suspend fun getLoanById(loanId: Long): LoanEntity?

    @Query("SELECT * FROM cuotas WHERE prestamo_id = :loanId ORDER BY numero_cuota ASC")
    fun getInstallmentsForLoan(loanId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM cuotas WHERE prestamo_id = :loanId ORDER BY numero_cuota ASC")
    suspend fun getInstallmentsListForLoan(loanId: Long): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Transaction
    suspend fun insertLoanWithSchedule(loan: LoanEntity, installments: List<InstallmentEntity>): Long {
        val loanId = insertLoan(loan)
        val updatedInstallments = installments.map { it.copy(prestamoId = loanId) }
        insertInstallments(updatedInstallments)
        return loanId
    }

    @Query("DELETE FROM cuotas WHERE prestamo_id = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: Long)

    @Transaction
    suspend fun updateLoanWithSchedule(loan: LoanEntity, installments: List<InstallmentEntity>) {
        updateLoan(loan)
        deleteInstallmentsForLoan(loan.id)
        val updatedInstallments = installments.map { it.copy(prestamoId = loan.id) }
        insertInstallments(updatedInstallments)
    }

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("SELECT COUNT(*) FROM prestamos WHERE estado = 'ACTIVE'")
    fun getActiveLoansCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM prestamos WHERE estado = 'OVERDUE'")
    fun getOverdueLoansCount(): Flow<Int>
}
