package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.DisbursementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisbursementDao {

    @Query("SELECT * FROM desembolsos ORDER BY fecha_programada DESC")
    fun getAllDisbursements(): Flow<List<DisbursementEntity>>

    @Query("SELECT * FROM desembolsos WHERE estado = 'PROGRAMADO' AND fecha_programada >= :startOfWeekMillis AND fecha_programada <= :endOfWeekMillis ORDER BY fecha_programada ASC")
    fun getWeeklyScheduledDisbursements(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<DisbursementEntity>>

    @Query("SELECT * FROM desembolsos WHERE prestamo_id = :loanId LIMIT 1")
    suspend fun getDisbursementForLoan(loanId: Long): DisbursementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisbursement(disbursement: DisbursementEntity): Long

    @Update
    suspend fun updateDisbursement(disbursement: DisbursementEntity)
}
