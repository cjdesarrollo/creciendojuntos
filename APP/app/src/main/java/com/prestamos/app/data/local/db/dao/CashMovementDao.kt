package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prestamos.app.data.local.db.entities.CashMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {

    @Query("SELECT * FROM movimientos_caja ORDER BY fecha_movimiento DESC")
    fun getAllMovements(): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM movimientos_caja WHERE fecha_movimiento >= :startOfDayMillis AND fecha_movimiento <= :endOfDayMillis ORDER BY fecha_movimiento DESC")
    fun getDailyMovements(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<CashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity): Long
}
