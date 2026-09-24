package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prestamos.app.data.local.db.entities.CashAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashAuditDao {

    @Query("SELECT * FROM arqueos_caja ORDER BY fecha_arqueo DESC")
    fun getAllAudits(): Flow<List<CashAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: CashAuditEntity): Long
}
