package com.prestamos.app.data.local.db.dao

import androidx.room.*
import com.prestamos.app.data.local.db.entities.CompanyInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyInfoDao {

    @Query("SELECT * FROM info_empresas")
    fun getAllCompanies(): Flow<List<CompanyInfoEntity>>

    @Query("SELECT * FROM info_empresas WHERE id = :id LIMIT 1")
    fun getCompanyById(id: Long): Flow<CompanyInfoEntity?>

    @Query("SELECT * FROM info_empresas WHERE id = :id LIMIT 1")
    suspend fun getCompanyByIdDirect(id: Long): CompanyInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompany(company: CompanyInfoEntity)

    @Update
    suspend fun updateCompany(company: CompanyInfoEntity)
}
