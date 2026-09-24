package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.GuaranteeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuaranteeDao {
    @Query("SELECT * FROM garantias WHERE prestamo_id = :prestamoId")
    fun getGuaranteesForLoan(prestamoId: Long): Flow<List<GuaranteeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuarantee(guarantee: GuaranteeEntity): Long

    @Update
    suspend fun updateGuarantee(guarantee: GuaranteeEntity)

    @Delete
    suspend fun deleteGuarantee(guarantee: GuaranteeEntity)
}
