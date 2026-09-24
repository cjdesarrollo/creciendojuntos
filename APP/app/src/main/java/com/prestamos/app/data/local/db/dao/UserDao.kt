package com.prestamos.app.data.local.db.dao

import androidx.room.*
import com.prestamos.app.data.local.db.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY id ASC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM usuarios WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE usuarios SET biometria_habilitada = :enabled WHERE id = :userId")
    suspend fun updateBiometricStatus(userId: Long, enabled: Boolean)

    @Query("UPDATE usuarios SET foto_uri = :photoUri WHERE id = :userId")
    suspend fun updateUserPhoto(userId: Long, photoUri: String?)
}
