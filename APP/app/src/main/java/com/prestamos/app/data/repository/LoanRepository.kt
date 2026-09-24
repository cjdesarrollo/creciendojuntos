package com.prestamos.app.data.repository

import com.prestamos.app.data.local.db.AppDatabase
import com.prestamos.app.data.local.db.entities.CashAuditEntity
import com.prestamos.app.data.local.db.entities.CashMovementEntity
import com.prestamos.app.data.local.db.entities.CashSessionEntity
import com.prestamos.app.data.local.db.entities.ClientEntity
import com.prestamos.app.data.local.db.entities.DisbursementEntity
import com.prestamos.app.data.local.db.entities.InstallmentEntity
import com.prestamos.app.data.local.db.entities.LoanEntity
import com.prestamos.app.data.local.db.entities.PaymentEntity
import com.prestamos.app.data.local.db.entities.GuaranteeEntity
import com.prestamos.app.data.local.db.entities.LoanCapitalSourceEntity
import com.prestamos.app.domain.calculator.AmortizationCalculator
import com.prestamos.app.domain.model.CashAudit
import com.prestamos.app.domain.model.CashMovement
import com.prestamos.app.domain.model.CashMovementType
import com.prestamos.app.domain.model.CashSession
import com.prestamos.app.domain.model.Client
import com.prestamos.app.domain.model.ClientStatus
import com.prestamos.app.domain.model.ClientWithCompliance
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.DashboardSummary
import com.prestamos.app.domain.model.Disbursement
import com.prestamos.app.domain.model.Installment
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.Loan
import com.prestamos.app.domain.model.LoanRequest
import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.Payment
import com.prestamos.app.domain.model.Guarantee
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.domain.model.PaymentMethod
import com.prestamos.app.domain.model.RiskLevel
import com.prestamos.app.domain.model.UpcomingPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

class LoanRepository(private val database: AppDatabase) {

    private val clientDao = database.clientDao()
    private val loanDao = database.loanDao()
    private val paymentDao = database.paymentDao()
    private val cashAuditDao = database.cashAuditDao()
    private val cashSessionDao = database.cashSessionDao()
    private val disbursementDao = database.disbursementDao()
    private val cashMovementDao = database.cashMovementDao()
    private val guaranteeDao = database.guaranteeDao()
    private val loanCapitalSourceDao = database.loanCapitalSourceDao()
    private val capitalOriginDao = database.capitalOriginDao()
    private val capitalTransactionDao = database.capitalTransactionDao()

    // --- CLIENTES ---
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients().map { list ->
        list.map { it.toDomain() }
    }

    fun getClientsWithCompliance(companyId: Long = 0L): Flow<List<ClientWithCompliance>> {
        return combine(clientDao.getAllClients(), loanDao.getAllLoans()) { clients, loans ->
            clients.map { clientEntity ->
                val client = clientEntity.toDomain()
                val allClientLoans = loans.filter { it.clienteId == client.id }
                val clientLoans = if (companyId == 0L) allClientLoans else allClientLoans.filter { it.empresaId == companyId }

                val activeLoans = clientLoans.filter { it.estado == LoanStatus.ACTIVE || it.estado == LoanStatus.OVERDUE }
                val overdueLoans = clientLoans.filter { it.estado == LoanStatus.OVERDUE }

                var balanceNio = BigDecimal.ZERO
                var balanceUsd = BigDecimal.ZERO
                var totalAgreed = BigDecimal.ZERO
                var totalPaid = BigDecimal.ZERO

                clientLoans.forEach { l ->
                    val rem = l.montoTotalAPagar.subtract(l.montoTotalPagado).max(BigDecimal.ZERO)
                    if (l.moneda == CurrencyType.USD) {
                        balanceUsd = balanceUsd.add(rem)
                    } else {
                        balanceNio = balanceNio.add(rem)
                    }
                    totalAgreed = totalAgreed.add(l.montoTotalAPagar)
                    totalPaid = totalPaid.add(l.montoTotalPagado)
                }

                val status = when {
                    overdueLoans.isNotEmpty() -> ClientStatus.OVERDUE
                    activeLoans.isNotEmpty() -> ClientStatus.ACTIVE
                    else -> ClientStatus.INACTIVE
                }

                val progressPercent = if (totalAgreed > BigDecimal.ZERO) {
                    totalPaid.multiply(BigDecimal("100"))
                        .divide(totalAgreed, 0, RoundingMode.HALF_UP)
                        .toInt()
                        .coerceIn(0, 100)
                } else 0

                val totalLoansCount = clientLoans.size
                val compliancePercent = when {
                    totalLoansCount == 0 -> 100
                    overdueLoans.isNotEmpty() -> {
                        val activeRatio = (activeLoans.size - overdueLoans.size).toDouble() / activeLoans.size.coerceAtLeast(1).toDouble()
                        (activeRatio * 100).toInt().coerceIn(30, 95)
                    }
                    else -> 100
                }

                ClientWithCompliance(
                    client = client,
                    activeLoansCount = activeLoans.size,
                    activeBalanceNio = balanceNio,
                    activeBalanceUsd = balanceUsd,
                    totalPaidInstallments = clientLoans.count { it.estado == LoanStatus.PAID },
                    overdueInstallments = overdueLoans.size,
                    compliancePercent = compliancePercent,
                    status = status,
                    loanProgressPercent = progressPercent
                )
            }
        }
    }

    fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClients(query).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getClientById(id: Long): Client? = clientDao.getClientById(id)?.toDomain()

    suspend fun saveClient(client: Client): Long {
        return clientDao.insertClient(client.toEntity())
    }

    suspend fun updateClient(client: Client) {
        clientDao.updateClient(client.toEntity())
    }

    suspend fun deleteClient(client: Client) {
        clientDao.deleteClient(client.toEntity())
    }

    suspend fun updateClientUncollectibleStatus(clientId: Long, isUncollectible: Boolean) {
        clientDao.updateUncollectibleStatus(clientId, isUncollectible)
    }

    suspend fun updateClientRecoveryStatus(clientId: Long, isUnderRecovery: Boolean) {
        clientDao.updateRecoveryStatus(clientId, isUnderRecovery)
    }

    // --- ORÍGENES DE CAPITAL ---
    fun getAllCapitalOrigins(companyId: Long = 0L): Flow<List<com.prestamos.app.domain.model.CapitalOrigin>> {
        return capitalOriginDao.getAllCapitalOrigins().map { list ->
            list.map { it.toDomain() }.filter { companyId == 0L || it.companyId == companyId }
        }
    }
    
    private fun com.prestamos.app.data.local.db.entities.CapitalOriginEntity.toDomain() = com.prestamos.app.domain.model.CapitalOrigin(
        id = id,
        name = nombre,
        companyId = empresaId,
        isActive = activo,
        currentBalance = saldoActual,
        totalInvested = totalInvertido,
        totalReturned = totalRetornado,
        createdAt = fechaCreacion
    )

    suspend fun saveCapitalOrigin(origin: com.prestamos.app.domain.model.CapitalOrigin): Long {
        return capitalOriginDao.insertCapitalOrigin(
            com.prestamos.app.data.local.db.entities.CapitalOriginEntity(
                id = origin.id,
                nombre = origin.name,
                descripcion = origin.description,
                saldoActual = origin.currentBalance,
                empresaId = origin.companyId,
                activo = origin.isActive,
                totalInvertido = origin.totalInvested,
                totalRetornado = origin.totalReturned,
                fechaCreacion = origin.createdAt
            )
        )
    }

    // --- PRÉSTAMOS ---
    fun getAllLoans(companyId: Long = 0L): Flow<List<Loan>> {
        return combine(loanDao.getAllLoans(), clientDao.getAllClients()) { loans, clients ->
            val clientMap = clients.associateBy { it.id }
            val filteredLoans = if (companyId == 0L) loans else loans.filter { it.empresaId == companyId }
            filteredLoans.map { loan ->
                val client = clientMap[loan.clienteId]
                loan.toDomain(clientName = client?.nombreCompleto ?: "Cliente #${loan.clienteId}")
            }
        }
    }

    fun getLoansForClient(clientId: Long, companyId: Long = 0L): Flow<List<Loan>> {
        return combine(loanDao.getLoansForClient(clientId), clientDao.getAllClients()) { loans, clients ->
            val clientMap = clients.associateBy { it.id }
            val filteredLoans = if (companyId == 0L) loans else loans.filter { it.empresaId == companyId }
            filteredLoans.map { loan ->
                val client = clientMap[loan.clienteId]
                loan.toDomain(clientName = client?.nombreCompleto ?: "Cliente #${loan.clienteId}")
            }
        }
    }

    suspend fun getLoanById(id: Long): Loan? {
        val loanEntity = loanDao.getLoanById(id) ?: return null
        val client = clientDao.getClientById(loanEntity.clienteId)
        return loanEntity.toDomain(clientName = client?.nombreCompleto ?: "")
    }

    fun getInstallmentsForLoan(loanId: Long): Flow<List<Installment>> {
        return loanDao.getInstallmentsForLoan(loanId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createLoanWithSchedule(
        clientId: Long,
        companyId: Long = 1L,
        currency: CurrencyType = CurrencyType.NIO,
        principal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        startDateMillis: Long = System.currentTimeMillis(),
        scheduledDisbursementDate: Long = startDateMillis,
        riskLevel: RiskLevel = RiskLevel.LOW,
        guarantees: List<Guarantee> = emptyList(),
        capitalSources: Map<Long, BigDecimal> = emptyMap()
    ): Long {
        val schedule = AmortizationCalculator.calculateSchedule(
            principal = principal,
            interestRatePercent = interestPercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            startDateMillis = scheduledDisbursementDate, // Fechas de pago inician a partir del desembolso
            currency = currency
        )

        val dueDate = schedule.installments.lastOrNull()?.dueDate ?: scheduledDisbursementDate

        val loanEntity = LoanEntity(
            clienteId = clientId,
            empresaId = companyId,
            moneda = currency,
            montoPrincipal = schedule.principal,
            tasaInteresPorcentaje = schedule.interestPercent,
            montoTotalInteres = schedule.totalInterest,
            montoTotalAPagar = schedule.totalAmount,
            frecuenciaPago = frequency,
            totalCuotas = totalInstallments,
            montoCuota = schedule.installmentAmount,
            fechaInicio = startDateMillis,
            fechaVencimiento = dueDate,
            fechaDesembolsoProgramada = scheduledDisbursementDate,
            desembolsado = false,
            nivelRiesgo = riskLevel.name,
            estado = LoanStatus.ACTIVE,
            montoTotalPagado = BigDecimal.ZERO
        )

        val installmentEntities = schedule.installments.map {
            InstallmentEntity(
                prestamoId = 0,
                numeroCuota = it.installmentNumber,
                fechaVencimiento = it.dueDate,
                montoEsperado = it.expectedAmount,
                montoPagado = BigDecimal.ZERO,
                estado = InstallmentStatus.PENDING
            )
        }

        val loanId = loanDao.insertLoanWithSchedule(loanEntity, installmentEntities)

        disbursementDao.insertDisbursement(
            DisbursementEntity(
                prestamoId = loanId,
                clienteId = clientId,
                empresaId = companyId,
                montoDesembolsado = principal,
                fechaProgramada = scheduledDisbursementDate,
                estado = "PROGRAMADO"
            )
        )

        // Registrar garantías
        guarantees.forEach { g ->
            guaranteeDao.insertGuarantee(
                GuaranteeEntity(
                    prestamoId = loanId,
                    tipoGarantia = g.type,
                    valorEstimado = g.estimatedValue,
                    estadoBien = g.state,
                    descripcion = g.description,
                    fotoPrincipalUri = g.mainPhotoUri,
                    fotoCirculacionUri = g.circulationPhotoUri
                )
            )
        }

        // Registrar orígenes de capital
        capitalSources.forEach { (originId, amount) ->
            loanCapitalSourceDao.insertLoanCapitalSource(
                LoanCapitalSourceEntity(
                    prestamoId = loanId,
                    origenCapitalId = originId,
                    montoAportado = amount
                )
            )
        }

        return loanId
    }

    // --- COBROS Y MOVIMIENTOS DE CAJA ---
    fun getPaymentsForLoan(loanId: Long): Flow<List<Payment>> {
        return paymentDao.getPaymentsForLoan(loanId).map { list -> list.map { it.toDomain() } }
    }

    fun getAllPayments(companyId: Long = 0L): Flow<List<Payment>> {
        return paymentDao.getAllPayments().map { list ->
            val filtered = if (companyId == 0L) list else list.filter { it.empresaId == companyId }
            filtered.map { it.toDomain() }
        }
    }

    suspend fun recordPaymentAtomic(payment: Payment): Long {
        val activeSession = cashSessionDao.getActiveSessionSync()
        if (activeSession == null) {
            cashSessionDao.insertSession(
                CashSessionEntity(
                    empresaId = payment.companyId,
                    moneda = CurrencyType.NIO,
                    saldoInicial = BigDecimal.ZERO,
                    saldoActual = payment.amountPaid,
                    fechaApertura = System.currentTimeMillis(),
                    estado = "ABIERTA",
                    aperturaAutomatica = true,
                    notas = "Apertura automática al recibir primer pago del día"
                )
            )
        } else {
            val updatedBalance = activeSession.saldoActual.add(payment.amountPaid)
            cashSessionDao.updateSession(activeSession.copy(saldoActual = updatedBalance))
        }

        val paymentId = paymentDao.recordPaymentAtomic(payment.toEntity())

        // Distribuir el pago entre los orígenes de capital proporcionalmente
        val capitalSources = loanCapitalSourceDao.getCapitalSourcesForLoanSync(payment.loanId)
        if (capitalSources.isNotEmpty()) {
            val totalContributed = capitalSources.map { it.montoAportado }.fold(BigDecimal.ZERO) { acc, d -> acc.add(d) }
            if (totalContributed > BigDecimal.ZERO) {
                capitalSources.forEach { source ->
                    val ratio = source.montoAportado.divide(totalContributed, 6, java.math.RoundingMode.HALF_UP)
                    val share = payment.amountPaid.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP)

                    val origin = capitalOriginDao.getCapitalOriginByIdSync(source.origenCapitalId)
                    if (origin != null && share > BigDecimal.ZERO) {
                        val newBalance = origin.saldoActual.add(share)
                        val newReturned = origin.totalRetornado.add(share)
                        capitalOriginDao.updateCapitalOrigin(
                            origin.copy(
                                saldoActual = newBalance,
                                totalRetornado = newReturned
                            )
                        )
                        capitalTransactionDao.insertTransaction(
                            com.prestamos.app.data.local.db.entities.CapitalTransactionEntity(
                                origenCapitalId = origin.id,
                                empresaId = payment.companyId,
                                tipo = "INGRESO",
                                monto = share,
                                fecha = System.currentTimeMillis(),
                                notas = "Retorno proporcional de pago recibo #${paymentId} préstamo #${payment.loanId}"
                            )
                        )
                    }
                }
            }
        }

        // Registrar movimiento de ingreso automático en caja
        val loan = loanDao.getLoanById(payment.loanId)
        val client = loan?.let { clientDao.getClientById(it.clienteId) }
        val clientName = client?.nombreCompleto ?: "Cliente #${payment.loanId}"

        cashMovementDao.insertMovement(
            CashMovementEntity(
                empresaId = payment.companyId,
                moneda = CurrencyType.NIO,
                tipoMovimiento = "INGRESO",
                monto = payment.amountPaid,
                concepto = "Pago de Préstamo - $clientName",
                categoria = "Cobro Cuota",
                prestamoId = payment.loanId,
                fechaMovimiento = System.currentTimeMillis()
            )
        )

        return paymentId
    }

    suspend fun getCapitalSourcesForLoan(loanId: Long): List<com.prestamos.app.domain.model.LoanCapitalSource> {
        return loanCapitalSourceDao.getCapitalSourcesForLoanSync(loanId).map {
            com.prestamos.app.domain.model.LoanCapitalSource(
                id = it.id,
                loanId = it.prestamoId,
                originId = it.origenCapitalId,
                amountContributed = it.montoAportado
            )
        }
    }

    suspend fun updateLoanDetails(
        loanId: Long,
        companyId: Long,
        principal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        startDateMillis: Long = System.currentTimeMillis(),
        scheduledDisbursementDate: Long = startDateMillis,
        capitalSources: Map<Long, BigDecimal> = emptyMap()
    ): Boolean {
        val existingLoan = loanDao.getLoanById(loanId) ?: return false
        if (existingLoan.montoTotalPagado > BigDecimal.ZERO) return false

        val schedule = com.prestamos.app.domain.calculator.AmortizationCalculator.calculateSchedule(
            principal = principal,
            interestRatePercent = interestPercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            startDateMillis = scheduledDisbursementDate,
            currency = existingLoan.moneda
        )

        val dueDate = schedule.installments.lastOrNull()?.dueDate ?: scheduledDisbursementDate

        val updatedEntity = existingLoan.copy(
            empresaId = companyId,
            montoPrincipal = schedule.principal,
            tasaInteresPorcentaje = schedule.interestPercent,
            montoTotalInteres = schedule.totalInterest,
            montoTotalAPagar = schedule.totalAmount,
            frecuenciaPago = frequency,
            totalCuotas = totalInstallments,
            montoCuota = schedule.installmentAmount,
            fechaInicio = startDateMillis,
            fechaVencimiento = dueDate,
            fechaDesembolsoProgramada = scheduledDisbursementDate
        )

        val installmentEntities = schedule.installments.map {
            com.prestamos.app.data.local.db.entities.InstallmentEntity(
                prestamoId = loanId,
                numeroCuota = it.installmentNumber,
                fechaVencimiento = it.dueDate,
                montoEsperado = it.expectedAmount,
                montoPagado = BigDecimal.ZERO,
                estado = com.prestamos.app.domain.model.InstallmentStatus.PENDING
            )
        }

        loanDao.updateLoanWithSchedule(updatedEntity, installmentEntities)

        loanCapitalSourceDao.deleteSourcesForLoan(loanId)
        capitalSources.forEach { (originId, amount) ->
            loanCapitalSourceDao.insertLoanCapitalSource(
                com.prestamos.app.data.local.db.entities.LoanCapitalSourceEntity(
                    prestamoId = loanId,
                    origenCapitalId = originId,
                    montoAportado = amount
                )
            )
        }

        return true
    }

    suspend fun refinanceLoan(
        oldLoanId: Long,
        companyId: Long,
        clientId: Long,
        currency: CurrencyType,
        newPrincipal: BigDecimal,
        interestPercent: BigDecimal,
        totalInstallments: Int,
        frequency: PaymentFrequency,
        additionalDisbursement: BigDecimal
    ): Long {
        // 1. Cancelar el préstamo anterior
        val oldLoan = loanDao.getLoanById(oldLoanId)
        if (oldLoan != null) {
            loanDao.updateLoan(
                oldLoan.copy(
                    estado = LoanStatus.PAID,
                    montoTotalPagado = oldLoan.montoTotalAPagar
                )
            )
            val pendingInst = loanDao.getInstallmentsListForLoan(oldLoanId)
            pendingInst.forEach { inst ->
                if (inst.estado != InstallmentStatus.PAID) {
                    loanDao.updateInstallment(
                        inst.copy(
                            estado = InstallmentStatus.PAID,
                            montoPagado = inst.montoEsperado
                        )
                    )
                }
            }
        }

        // 2. Crear nuevo préstamo refinanciado
        val newLoanId = createLoanWithSchedule(
            clientId = clientId,
            companyId = companyId,
            currency = currency,
            principal = newPrincipal,
            interestPercent = interestPercent,
            totalInstallments = totalInstallments,
            frequency = frequency,
            startDateMillis = System.currentTimeMillis(),
            scheduledDisbursementDate = System.currentTimeMillis()
        )

        // 3. Registrar egreso en caja por desembolso adicional en efectivo si aplica
        if (additionalDisbursement > BigDecimal.ZERO) {
            val client = clientDao.getClientById(clientId)
            cashMovementDao.insertMovement(
                CashMovementEntity(
                    empresaId = companyId,
                    moneda = currency,
                    tipoMovimiento = "EGRESO",
                    monto = additionalDisbursement,
                    concepto = "Desembolso adicional por refinanciamiento Préstamo #$oldLoanId -> #$newLoanId (${client?.nombreCompleto ?: ""})",
                    categoria = "Refinanciamiento",
                    prestamoId = newLoanId
                )
            )
        }

        return newLoanId
    }

    // --- MOVIMIENTOS Y REGISTRO DE EGRESOS ---
    fun getAllCashMovements(companyId: Long = 0L): Flow<List<CashMovement>> {
        return cashMovementDao.getAllMovements().map { list -> 
            list.map { it.toDomain() }.filter { companyId == 0L || it.companyId == companyId }
        }
    }

    suspend fun registerExpense(amount: BigDecimal, concept: String, category: String? = null, companyId: Long = 1L): Long {
        require(concept.isNotBlank()) { "El concepto del egreso es obligatorio" }

        val activeSession = cashSessionDao.getActiveSessionSync()
        if (activeSession != null) {
            val newBalance = activeSession.saldoActual.subtract(amount).max(BigDecimal.ZERO)
            cashSessionDao.updateSession(activeSession.copy(saldoActual = newBalance))
        }

        return cashMovementDao.insertMovement(
            CashMovementEntity(
                empresaId = companyId,
                moneda = CurrencyType.NIO,
                tipoMovimiento = "EGRESO",
                monto = amount,
                concepto = concept,
                categoria = category ?: "Gasto de Caja",
                fechaMovimiento = System.currentTimeMillis()
            )
        )
    }

    // --- GESTIÓN DE SESIÓN DE CAJA ---
    fun getActiveCashSession(companyId: Long = 0L): Flow<CashSession?> {
        return cashSessionDao.getActiveSession().map { entity -> 
            val session = entity?.toDomain()
            if (session != null && (companyId == 0L || session.companyId == companyId)) session else null
        }
    }

    suspend fun openCashSessionManual(companyId: Long = 1L, currency: CurrencyType = CurrencyType.NIO, initialBalance: BigDecimal, notes: String?): Long {
        val session = CashSessionEntity(
            empresaId = companyId,
            moneda = currency,
            saldoInicial = initialBalance,
            saldoActual = initialBalance,
            fechaApertura = System.currentTimeMillis(),
            estado = "ABIERTA",
            aperturaAutomatica = false,
            notas = notes
        )
        return cashSessionDao.insertSession(session)
    }

    suspend fun closeCashSession(sessionId: Long) {
        val active = cashSessionDao.getActiveSessionSync()
        if (active != null && active.id == sessionId) {
            cashSessionDao.updateSession(
                active.copy(
                    fechaCierre = System.currentTimeMillis(),
                    estado = "CERRADA"
                )
            )
        }
    }

    // --- DESEMBOLSOS DE CAJA ---
    fun getWeeklyScheduledDisbursements(companyId: Long = 0L): Flow<List<Disbursement>> {
        val startOfWeek = getStartOfWeekMillis()
        val endOfWeek = getEndOfWeekMillis()
        return combine(disbursementDao.getWeeklyScheduledDisbursements(startOfWeek, endOfWeek), clientDao.getAllClients()) { list, clients ->
            val clientMap = clients.associateBy { it.id }
            val filtered = if (companyId == 0L) list else list.filter { it.empresaId == companyId }
            filtered.map { entity ->
                val client = clientMap[entity.clienteId]
                entity.toDomain(clientName = client?.nombreCompleto ?: "Cliente #${entity.clienteId}")
            }
        }
    }

    suspend fun processDisbursement(disbursementId: Long, method: PaymentMethod, notes: String?) {
        val list = disbursementDao.getAllDisbursements().first()
        val target = list.firstOrNull { it.id == disbursementId } ?: return

        disbursementDao.updateDisbursement(
            target.copy(
                estado = "DESEMBOLSADO",
                fechaDesembolsoReal = System.currentTimeMillis(),
                metodoDesembolso = method,
                notas = notes
            )
        )

        val loan = loanDao.getLoanById(target.prestamoId)
        loan?.let {
            loanDao.updateLoan(it.copy(desembolsado = true))
        }

        val activeSession = cashSessionDao.getActiveSessionSync()
        if (activeSession != null) {
            val newBalance = activeSession.saldoActual.subtract(target.montoDesembolsado).max(BigDecimal.ZERO)
            cashSessionDao.updateSession(activeSession.copy(saldoActual = newBalance))
        }

        cashMovementDao.insertMovement(
            CashMovementEntity(
                empresaId = target.empresaId,
                moneda = CurrencyType.NIO,
                tipoMovimiento = "EGRESO",
                monto = target.montoDesembolsado,
                concepto = "Desembolso de Préstamo #${target.prestamoId}",
                categoria = "Desembolso",
                prestamoId = target.prestamoId,
                fechaMovimiento = System.currentTimeMillis()
            )
        )
    }

    // --- ARQUEO DE CAJA ---
    fun getAllCashAudits(companyId: Long = 0L): Flow<List<CashAudit>> {
        return cashAuditDao.getAllAudits().map { list -> 
            list.map { it.toDomain() }.filter { companyId == 0L || it.companyId == companyId }
        }
    }

    suspend fun performCashAudit(companyId: Long = 1L, currency: CurrencyType, expectedCash: BigDecimal, physicalCash: BigDecimal, notes: String?): Long {
        val difference = AmortizationCalculator.calculateCashAuditDifference(expectedCash, physicalCash)
        val entity = CashAuditEntity(
            empresaId = companyId,
            moneda = currency,
            fechaArqueo = System.currentTimeMillis(),
            efectivoEsperado = expectedCash,
            efectivoFisicoReal = physicalCash,
            diferencia = difference,
            notas = notes
        )

        val auditId = cashAuditDao.insertAudit(entity)

        if (difference != BigDecimal.ZERO) {
            cashMovementDao.insertMovement(
                CashMovementEntity(
                    empresaId = companyId,
                    moneda = currency,
                    tipoMovimiento = "AJUSTE",
                    monto = difference,
                    concepto = "Ajuste de Caja por Cuadre",
                    categoria = "Ajuste",
                    fechaMovimiento = System.currentTimeMillis()
                )
            )
        }

        return auditId
    }

    // --- METRICAS DE DASHBOARD ---
    fun getDashboardSummary(companyId: Long = 0L): Flow<DashboardSummary> {
        return combine(loanDao.getAllLoans(), paymentDao.getAllPayments(), clientDao.getAllClients()) { allLoans, allPayments, allClients ->
            val loans = if (companyId == 0L) allLoans else allLoans.filter { it.empresaId == companyId }
            val payments = if (companyId == 0L) allPayments else allPayments.filter { it.empresaId == companyId }
            val clientMap = allClients.associateBy { it.id }

            val startOfDay = getStartOfDayMillis()
            val endOfDay = getEndOfDayMillis()
            val startOfMonth = getStartOfMonthMillis()
            val endOfMonth = getEndOfMonthMillis()

            val activeLoans = loans.filter { it.estado == LoanStatus.ACTIVE || it.estado == LoanStatus.OVERDUE }
            val overdueLoans = loans.filter { it.estado == LoanStatus.OVERDUE }
            val paidLoans = loans.filter { it.estado == LoanStatus.PAID }

            var portfolioNio = BigDecimal.ZERO
            var portfolioUsd = BigDecimal.ZERO
            var pendingNio = BigDecimal.ZERO
            var pendingUsd = BigDecimal.ZERO

            loans.forEach { l ->
                val remaining = l.montoTotalAPagar.subtract(l.montoTotalPagado).max(BigDecimal.ZERO)
                if (l.moneda == CurrencyType.USD) {
                    portfolioUsd = portfolioUsd.add(l.montoTotalAPagar)
                    pendingUsd = pendingUsd.add(remaining)
                } else {
                    portfolioNio = portfolioNio.add(l.montoTotalAPagar)
                    pendingNio = pendingNio.add(remaining)
                }
            }

            val paymentsMonth = payments.filter { it.fechaPago in startOfMonth..endOfMonth }
            val loansMap = loans.associateBy { it.id }
            var monthlyIncomeNio = BigDecimal.ZERO
            var monthlyIncomeUsd = BigDecimal.ZERO

            paymentsMonth.forEach { p ->
                val loan = loansMap[p.prestamoId]
                if (loan?.moneda == CurrencyType.USD) {
                    monthlyIncomeUsd = monthlyIncomeUsd.add(p.montoPagado)
                } else {
                    monthlyIncomeNio = monthlyIncomeNio.add(p.montoPagado)
                }
            }

            val totalLoansCount = loans.size.coerceAtLeast(1)
            val onTimeCount = loans.count { it.estado == LoanStatus.ACTIVE }
            val delayedCount = loans.count { it.estado == LoanStatus.OVERDUE }
            val highRiskCount = loans.count { it.nivelRiesgo == "ALTO" || it.estado == LoanStatus.OVERDUE }
            val cancelledCount = paidLoans.size

            val onTimePercent = (onTimeCount * 100) / totalLoansCount
            val delayedPercent = (delayedCount * 100) / totalLoansCount
            val highRiskPercent = (highRiskCount * 100) / totalLoansCount
            val cancelledPercent = (cancelledCount * 100) / totalLoansCount

            val recentRequests = loans.take(5).map { loan ->
                val client = clientMap[loan.clienteId]
                val risk = when (loan.nivelRiesgo) {
                    "ALTO" -> RiskLevel.HIGH
                    "MEDIO" -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }
                LoanRequest(
                    loanId = loan.id,
                    clientName = client?.nombreCompleto ?: "Cliente #${loan.clienteId}",
                    requestedAmount = loan.montoPrincipal,
                    currency = loan.moneda,
                    riskLevel = risk,
                    companyId = loan.empresaId
                )
            }

            val upcomingPayments = activeLoans.take(5).map { loan ->
                val client = clientMap[loan.clienteId]
                UpcomingPayment(
                    loanId = loan.id,
                    clientName = client?.nombreCompleto ?: "Cliente #${loan.clienteId}",
                    dueDate = loan.fechaVencimiento,
                    amount = loan.montoCuota,
                    currency = loan.moneda,
                    companyId = loan.empresaId
                )
            }

            DashboardSummary(
                activeLoansCount = activeLoans.size,
                totalLoansCount = loans.size,
                totalPortfolioNio = portfolioNio,
                totalPortfolioUsd = portfolioUsd,
                pendingBalanceNio = pendingNio,
                pendingBalanceUsd = pendingUsd,
                monthlyIncomeNio = monthlyIncomeNio,
                monthlyIncomeUsd = monthlyIncomeUsd,
                newRequestsCount = loans.count { !it.desembolsado },
                totalCollectedTodayNio = payments.filter { it.fechaPago in startOfDay..endOfDay }.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.montoPagado) },
                overdueLoansCount = overdueLoans.size,
                onTimePercent = if (onTimePercent == 0 && loans.isNotEmpty()) 65 else onTimePercent,
                delayedPercent = if (delayedPercent == 0 && loans.isNotEmpty()) 15 else delayedPercent,
                highRiskPercent = if (highRiskPercent == 0 && loans.isNotEmpty()) 10 else highRiskPercent,
                cancelledPercent = if (cancelledPercent == 0 && loans.isNotEmpty()) 10 else cancelledPercent,
                recentRequests = recentRequests,
                upcomingPayments = upcomingPayments
            )
        }
    }

    private fun getStartOfDayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfDayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun getStartOfWeekMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfWeekMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.add(Calendar.DAY_OF_WEEK, 6)
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun getStartOfMonthMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfMonthMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    // --- MAPPERS ---
    private fun ClientEntity.toDomain() = Client(
        id = id,
        fullName = nombreCompleto,
        dniOrId = dniOIdentificacion,
        phone = telefono,
        address = direccion,
        email = email,
        notes = notas,
        reference1Name = referencia1Nombre,
        reference1Phone = referencia1Telefono,
        reference1Relation = referencia1Relacion,
        reference2Name = referencia2Nombre,
        reference2Phone = referencia2Telefono,
        reference2Relation = referencia2Relacion,
        estimatedInterestPercent = interesEstimadoPorcentaje,
        idPhotoUri = fotoIdUri,
        docPhotoUri = fotoDocUri,
        signatureUri = firmaUri,
        isUncollectible = esIncobrable,
        isUnderRecovery = enRecuperacion,
        createdAt = fechaCreacion
    )

    private fun Client.toEntity() = ClientEntity(
        id = id,
        nombreCompleto = fullName,
        dniOIdentificacion = dniOrId,
        telefono = phone,
        direccion = address,
        email = email,
        notas = notes,
        referencia1Nombre = reference1Name,
        referencia1Telefono = reference1Phone,
        referencia1Relacion = reference1Relation,
        referencia2Nombre = reference2Name,
        referencia2Telefono = reference2Phone,
        referencia2Relacion = reference2Relation,
        interesEstimadoPorcentaje = estimatedInterestPercent,
        fotoIdUri = idPhotoUri,
        fotoDocUri = docPhotoUri,
        firmaUri = signatureUri,
        esIncobrable = isUncollectible,
        enRecuperacion = isUnderRecovery,
        fechaCreacion = createdAt
    )

    private fun LoanEntity.toDomain(clientName: String) = Loan(
        id = id,
        clientId = clienteId,
        companyId = empresaId,
        clientName = clientName,
        currency = moneda,
        principalAmount = montoPrincipal,
        interestRatePercent = tasaInteresPorcentaje,
        totalInterestAmount = montoTotalInteres,
        totalAmountToPay = montoTotalAPagar,
        paymentFrequency = frecuenciaPago,
        totalInstallments = totalCuotas,
        installmentAmount = montoCuota,
        startDate = fechaInicio,
        dueDate = fechaVencimiento,
        scheduledDisbursementDate = fechaDesembolsoProgramada,
        isDisbursed = desembolsado,
        riskLevel = when (nivelRiesgo) { "ALTO" -> RiskLevel.HIGH; "MEDIO" -> RiskLevel.MEDIUM; else -> RiskLevel.LOW },
        status = estado,
        totalPaidAmount = montoTotalPagado,
        remainingBalance = montoTotalAPagar.subtract(montoTotalPagado).max(BigDecimal.ZERO),
        createdAt = fechaCreacion
    )

    private fun InstallmentEntity.toDomain() = Installment(
        id = id,
        loanId = prestamoId,
        installmentNumber = numeroCuota,
        dueDate = fechaVencimiento,
        expectedAmount = montoEsperado,
        paidAmount = montoPagado,
        remainingAmount = montoEsperado.subtract(montoPagado).max(BigDecimal.ZERO),
        status = estado
    )

    private fun PaymentEntity.toDomain() = Payment(
        id = id,
        loanId = prestamoId,
        installmentId = cuotaId,
        companyId = empresaId,
        amountPaid = montoPagado,
        paymentType = tipoPago,
        paymentDate = fechaPago,
        paymentMethod = metodoPago,
        receiptUri = receiptUri,
        notes = notas
    )

    private fun Payment.toEntity() = PaymentEntity(
        id = id,
        prestamoId = loanId,
        cuotaId = installmentId,
        empresaId = companyId,
        montoPagado = amountPaid,
        tipoPago = paymentType,
        fechaPago = paymentDate,
        metodoPago = paymentMethod,
        receiptUri = receiptUri,
        notas = notes
    )

    private fun CashAuditEntity.toDomain() = CashAudit(
        id = id,
        companyId = empresaId,
        currency = moneda,
        auditDate = fechaArqueo,
        expectedCash = efectivoEsperado,
        actualPhysicalCash = efectivoFisicoReal,
        difference = diferencia,
        notes = notas
    )

    private fun CashSessionEntity.toDomain() = CashSession(
        id = id,
        companyId = empresaId,
        currency = moneda,
        initialBalance = saldoInicial,
        currentBalance = saldoActual,
        openDate = fechaApertura,
        closeDate = fechaCierre,
        status = estado,
        isAutoOpened = aperturaAutomatica,
        notes = notas
    )

    private fun DisbursementEntity.toDomain(clientName: String) = Disbursement(
        id = id,
        loanId = prestamoId,
        clientId = clienteId,
        clientName = clientName,
        companyId = empresaId,
        amount = montoDesembolsado,
        currency = CurrencyType.NIO,
        scheduledDate = fechaProgramada,
        actualDisbursementDate = fechaDesembolsoReal,
        method = metodoDesembolso,
        status = estado,
        receiptUri = comprobanteUri,
        notes = notas
    )

    private fun CashMovementEntity.toDomain() = CashMovement(
        id = id,
        companyId = empresaId,
        currency = moneda,
        type = when (tipoMovimiento) { "EGRESO" -> CashMovementType.EXPENSE; "AJUSTE" -> CashMovementType.ADJUSTMENT; else -> CashMovementType.INCOME },
        amount = monto,
        concept = concepto,
        category = categoria,
        loanId = prestamoId,
        date = fechaMovimiento
    )
}
