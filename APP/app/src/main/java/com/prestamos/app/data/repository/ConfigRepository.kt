package com.prestamos.app.data.repository

import com.prestamos.app.data.local.db.dao.CompanyInfoDao
import com.prestamos.app.data.local.db.dao.UserDao
import com.prestamos.app.data.local.db.entities.CompanyInfoEntity
import com.prestamos.app.data.local.db.entities.UserEntity
import com.prestamos.app.domain.model.AppUser
import com.prestamos.app.domain.model.CompanyProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepository(
    private val userDao: UserDao,
    private val companyInfoDao: CompanyInfoDao
) {

    // --- USUARIOS ---

    fun getAllActiveUsers(): Flow<List<AppUser>> {
        return userDao.getAllActiveUsers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getUserById(id: Long): AppUser? {
        return userDao.getUserById(id)?.toDomain()
    }

    suspend fun saveUser(user: AppUser): Long {
        val entity = user.toEntity()
        return if (user.id == 0L) {
            userDao.insertUser(entity)
        } else {
            userDao.updateUser(entity)
            user.id
        }
    }

    suspend fun deleteUser(user: AppUser) {
        userDao.deleteUser(user.toEntity())
    }

    suspend fun setBiometricStatus(userId: Long, enabled: Boolean) {
        userDao.updateBiometricStatus(userId, enabled)
    }

    suspend fun updateUserPhoto(userId: Long, photoUri: String?) {
        userDao.updateUserPhoto(userId, photoUri)
    }

    // --- EMPRESAS ---

    fun getAllCompanies(): Flow<List<CompanyProfile>> {
        return companyInfoDao.getAllCompanies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getCompanyById(id: Long): Flow<CompanyProfile?> {
        return companyInfoDao.getCompanyById(id).map { it?.toDomain() }
    }

    suspend fun saveCompany(company: CompanyProfile) {
        companyInfoDao.insertOrUpdateCompany(company.toEntity())
    }

    // --- MAPEOS ---

    private fun UserEntity.toDomain() = AppUser(
        id = id,
        fullName = nombreCompleto,
        username = username,
        phone = telefono,
        role = rol,
        pin = pin,
        photoUri = fotoUri,
        biometricEnabled = biometriaHabilitada,
        isActive = activo,
        createdAt = fechaCreacion
    )

    private fun AppUser.toEntity() = UserEntity(
        id = id,
        nombreCompleto = fullName,
        username = username,
        telefono = phone,
        rol = role,
        pin = pin,
        fotoUri = photoUri,
        biometriaHabilitada = biometricEnabled,
        activo = isActive,
        fechaCreacion = createdAt
    )

    private fun CompanyInfoEntity.toDomain() = CompanyProfile(
        id = id,
        name = nombre,
        phone = telefono,
        address = direccion,
        ruc = ruc,
        email = email,
        defaultInterestRate = tasaInteresDefecto
    )

    private fun CompanyProfile.toEntity() = CompanyInfoEntity(
        id = id,
        nombre = name,
        telefono = phone,
        direccion = address,
        ruc = ruc,
        email = email,
        tasaInteresDefecto = defaultInterestRate
    )
}
