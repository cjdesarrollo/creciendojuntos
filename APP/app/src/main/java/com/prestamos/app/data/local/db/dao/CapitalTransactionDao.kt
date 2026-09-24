package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.CapitalTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapitalTransactionDao {
    @Query("SELECT * FROM transacciones_capital WHERE origen_capital_id = :origenId ORDER BY fecha DESC")
    fun getTransactionsForOrigin(origenId: Long): Flow<List<CapitalTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CapitalTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: CapitalTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: CapitalTransactionEntity)
}
