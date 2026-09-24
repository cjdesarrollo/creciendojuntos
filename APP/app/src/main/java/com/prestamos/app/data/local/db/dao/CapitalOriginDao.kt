package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.CapitalOriginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapitalOriginDao {
    @Query("SELECT * FROM origenes_capital")
    fun getAllCapitalOrigins(): Flow<List<CapitalOriginEntity>>

    @Query("SELECT * FROM origenes_capital WHERE id = :id")
    fun getCapitalOriginById(id: Long): Flow<CapitalOriginEntity?>

    @Query("SELECT * FROM origenes_capital WHERE id = :id LIMIT 1")
    suspend fun getCapitalOriginByIdSync(id: Long): CapitalOriginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapitalOrigin(origin: CapitalOriginEntity): Long

    @Update
    suspend fun updateCapitalOrigin(origin: CapitalOriginEntity)

    @Delete
    suspend fun deleteCapitalOrigin(origin: CapitalOriginEntity)
}
