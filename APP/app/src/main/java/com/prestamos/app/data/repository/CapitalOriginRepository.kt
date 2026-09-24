package com.prestamos.app.data.repository

import com.prestamos.app.data.local.db.dao.CapitalOriginDao
import com.prestamos.app.data.local.db.dao.CapitalTransactionDao
import com.prestamos.app.data.local.db.entities.CapitalOriginEntity
import com.prestamos.app.data.local.db.entities.CapitalTransactionEntity
import com.prestamos.app.domain.model.CapitalOrigin
import com.prestamos.app.domain.model.CapitalTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class CapitalOriginRepository(
    private val originDao: CapitalOriginDao,
    private val transactionDao: CapitalTransactionDao
) {
    fun getAllOrigins(): Flow<List<CapitalOrigin>> {
        return originDao.getAllCapitalOrigins().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getOriginById(id: Long): Flow<CapitalOrigin?> {
        return originDao.getCapitalOriginById(id).map { it?.toDomain() }
    }

    suspend fun createOrigin(
        name: String,
        description: String? = null,
        companyId: Long = 0L,
        initialBalance: BigDecimal = BigDecimal.ZERO
    ): Long {
        val entity = CapitalOriginEntity(
            nombre = name,
            descripcion = description,
            empresaId = companyId,
            saldoActual = initialBalance,
            totalInvertido = initialBalance,
            activo = true
        )
        val id = originDao.insertCapitalOrigin(entity)
        if (initialBalance > BigDecimal.ZERO) {
            val transaction = CapitalTransactionEntity(
                origenCapitalId = id,
                empresaId = if (companyId > 0L) companyId else null,
                tipo = "INGRESO",
                monto = initialBalance,
                notas = "Aporte de capital inicial"
            )
            transactionDao.insertTransaction(transaction)
        }
        return id
    }

    suspend fun updateOrigin(origin: CapitalOrigin) {
        val existing = originDao.getCapitalOriginByIdSync(origin.id)
        if (existing != null) {
            val updated = existing.copy(
                nombre = origin.name,
                descripcion = origin.description,
                empresaId = origin.companyId,
                activo = origin.isActive
            )
            originDao.updateCapitalOrigin(updated)
        }
    }

    suspend fun increaseCapital(
        originId: Long,
        amount: BigDecimal,
        notes: String? = null,
        companyId: Long? = null
    ): Boolean {
        if (amount <= BigDecimal.ZERO) return false
        val existing = originDao.getCapitalOriginByIdSync(originId) ?: return false
        val newBalance = existing.saldoActual.add(amount)
        val newTotalInvested = existing.totalInvertido.add(amount)
        val updated = existing.copy(
            saldoActual = newBalance,
            totalInvertido = newTotalInvested
        )
        originDao.updateCapitalOrigin(updated)

        val transaction = CapitalTransactionEntity(
            origenCapitalId = originId,
            empresaId = companyId ?: if (existing.empresaId > 0L) existing.empresaId else null,
            tipo = "INGRESO",
            monto = amount,
            notas = notes ?: "Aumento de capital"
        )
        transactionDao.insertTransaction(transaction)
        return true
    }

    suspend fun addTransaction(originId: Long, companyId: Long?, type: String, amount: BigDecimal, notes: String?) {
        val existing = originDao.getCapitalOriginByIdSync(originId)
        if (existing != null) {
            val newBalance = if (type == "INGRESO") {
                existing.saldoActual.add(amount)
            } else {
                existing.saldoActual.subtract(amount)
            }
            val newInvested = if (type == "INGRESO") existing.totalInvertido.add(amount) else existing.totalInvertido
            val newReturned = if (type == "EGRESO") existing.totalRetornado.add(amount) else existing.totalRetornado
            originDao.updateCapitalOrigin(
                existing.copy(
                    saldoActual = newBalance,
                    totalInvertido = newInvested,
                    totalRetornado = newReturned
                )
            )
        }

        val transaction = CapitalTransactionEntity(
            origenCapitalId = originId,
            empresaId = companyId,
            tipo = type,
            monto = amount,
            notas = notes
        )
        transactionDao.insertTransaction(transaction)
    }

    fun getTransactionsForOrigin(originId: Long): Flow<List<CapitalTransaction>> {
        return transactionDao.getTransactionsForOrigin(originId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun deleteOrigin(originId: Long): Boolean {
        val existing = originDao.getCapitalOriginByIdSync(originId) ?: return false
        originDao.deleteCapitalOrigin(existing)
        return true
    }

    private fun CapitalOriginEntity.toDomain() = CapitalOrigin(
        id = id,
        name = nombre,
        description = descripcion,
        currentBalance = saldoActual,
        companyId = empresaId,
        isActive = activo,
        totalInvested = totalInvertido,
        totalReturned = totalRetornado,
        createdAt = fechaCreacion
    )

    private fun CapitalTransactionEntity.toDomain() = CapitalTransaction(
        id = id,
        originId = origenCapitalId,
        companyId = empresaId,
        type = tipo,
        amount = monto,
        date = fecha,
        notes = notas
    )
}
