package com.prestamos.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prestamos.app.data.local.db.entities.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Query("SELECT * FROM clientes ORDER BY nombre_completo ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clientes WHERE id = :clientId")
    suspend fun getClientById(clientId: Long): ClientEntity?

    @Query("SELECT * FROM clientes WHERE nombre_completo LIKE '%' || :query || '%' OR dni_o_identificacion LIKE '%' || :query || '%' ORDER BY nombre_completo ASC")
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>): List<Long>

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("UPDATE clientes SET es_incobrable = :isUncollectible WHERE id = :clientId")
    suspend fun updateUncollectibleStatus(clientId: Long, isUncollectible: Boolean)

    @Query("UPDATE clientes SET en_recuperacion = :isUnderRecovery WHERE id = :clientId")
    suspend fun updateRecoveryStatus(clientId: Long, isUnderRecovery: Boolean)

    @Query("SELECT COUNT(*) FROM clientes")
    fun getClientsCount(): Flow<Int>
}
