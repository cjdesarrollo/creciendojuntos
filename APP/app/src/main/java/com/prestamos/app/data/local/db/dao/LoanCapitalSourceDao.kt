package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prestamos.app.data.local.db.entities.LoanCapitalSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanCapitalSourceDao {
    @Query("SELECT * FROM prestamo_origenes_capital WHERE prestamo_id = :prestamoId")
    fun getCapitalSourcesForLoan(prestamoId: Long): Flow<List<LoanCapitalSourceEntity>>

    @Query("SELECT * FROM prestamo_origenes_capital WHERE prestamo_id = :prestamoId")
    suspend fun getCapitalSourcesForLoanSync(prestamoId: Long): List<LoanCapitalSourceEntity>

    @Query("DELETE FROM prestamo_origenes_capital WHERE prestamo_id = :prestamoId")
    suspend fun deleteSourcesForLoan(prestamoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanCapitalSource(source: LoanCapitalSourceEntity): Long
}
