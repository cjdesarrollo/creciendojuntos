package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.CashSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {

    @Query("SELECT * FROM sesiones_caja WHERE estado = 'ABIERTA' ORDER BY fecha_apertura DESC LIMIT 1")
    fun getActiveSession(): Flow<CashSessionEntity?>

    @Query("SELECT * FROM sesiones_caja WHERE estado = 'ABIERTA' ORDER BY fecha_apertura DESC LIMIT 1")
    suspend fun getActiveSessionSync(): CashSessionEntity?

    @Query("SELECT * FROM sesiones_caja ORDER BY fecha_apertura DESC")
    fun getAllSessions(): Flow<List<CashSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashSessionEntity)
}
