package com.prestamos.app.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.InstallmentStatus
import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.domain.model.PaymentMethod
import java.math.BigDecimal

@Entity(tableName = "clientes")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "nombre_completo")
    val nombreCompleto: String,

    @ColumnInfo(name = "dni_o_identificacion")
    val dniOIdentificacion: String,

    @ColumnInfo(name = "telefono")
    val telefono: String,

    @ColumnInfo(name = "direccion")
    val direccion: String,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "notas")
    val notas: String? = null,

    @ColumnInfo(name = "referencia1_nombre")
    val referencia1Nombre: String? = null,

    @ColumnInfo(name = "referencia1_telefono")
    val referencia1Telefono: String? = null,

    @ColumnInfo(name = "referencia1_relacion")
    val referencia1Relacion: String? = null,

    @ColumnInfo(name = "referencia2_nombre")
    val referencia2Nombre: String? = null,

    @ColumnInfo(name = "referencia2_telefono")
    val referencia2Telefono: String? = null,

    @ColumnInfo(name = "referencia2_relacion")
    val referencia2Relacion: String? = null,

    @ColumnInfo(name = "interes_estimado_porcentaje")
    val interesEstimadoPorcentaje: BigDecimal? = null,

    @ColumnInfo(name = "foto_id_uri")
    val fotoIdUri: String? = null,

    @ColumnInfo(name = "foto_doc_uri")
    val fotoDocUri: String? = null,

    @ColumnInfo(name = "firma_uri")
    val firmaUri: String? = null,

    @ColumnInfo(name = "es_incobrable")
    val esIncobrable: Boolean = false,

    @ColumnInfo(name = "en_recuperacion")
    val enRecuperacion: Boolean = false,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prestamos",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cliente_id"])]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "moneda")
    val moneda: CurrencyType = CurrencyType.NIO,

    @ColumnInfo(name = "monto_principal")
    val montoPrincipal: BigDecimal,

    @ColumnInfo(name = "tasa_interes_porcentaje")
    val tasaInteresPorcentaje: BigDecimal,

    @ColumnInfo(name = "monto_total_interes")
    val montoTotalInteres: BigDecimal,

    @ColumnInfo(name = "monto_total_a_pagar")
    val montoTotalAPagar: BigDecimal,

    @ColumnInfo(name = "frecuencia_pago")
    val frecuenciaPago: PaymentFrequency,

    @ColumnInfo(name = "total_cuotas")
    val totalCuotas: Int,

    @ColumnInfo(name = "monto_cuota")
    val montoCuota: BigDecimal,

    @ColumnInfo(name = "fecha_inicio")
    val fechaInicio: Long,

    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: Long,

    @ColumnInfo(name = "fecha_desembolso_programada")
    val fechaDesembolsoProgramada: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "desembolsado")
    val desembolsado: Boolean = false,

    @ColumnInfo(name = "nivel_riesgo")
    val nivelRiesgo: String = "BAJO",

    @ColumnInfo(name = "estado")
    val estado: LoanStatus = LoanStatus.ACTIVE,

    @ColumnInfo(name = "monto_total_pagado")
    val montoTotalPagado: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cuotas",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["prestamo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prestamo_id"])]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long,

    @ColumnInfo(name = "numero_cuota")
    val numeroCuota: Int,

    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: Long,

    @ColumnInfo(name = "monto_esperado")
    val montoEsperado: BigDecimal,

    @ColumnInfo(name = "monto_pagado")
    val montoPagado: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "estado")
    val estado: InstallmentStatus = InstallmentStatus.PENDING
)

@Entity(
    tableName = "pagos",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["prestamo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prestamo_id"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long,

    @ColumnInfo(name = "cuota_id")
    val cuotaId: Long? = null,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "monto_pagado")
    val montoPagado: BigDecimal,

    @ColumnInfo(name = "tipo_pago")
    val tipoPago: String = "CUOTA",

    @ColumnInfo(name = "fecha_pago")
    val fechaPago: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: PaymentMethod = PaymentMethod.CASH,

    @ColumnInfo(name = "recibo_uri")
    val receiptUri: String? = null,

    @ColumnInfo(name = "notas")
    val notas: String? = null
)

@Entity(tableName = "arqueos_caja")
data class CashAuditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "moneda")
    val moneda: CurrencyType = CurrencyType.NIO,

    @ColumnInfo(name = "fecha_arqueo")
    val fechaArqueo: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "efectivo_esperado")
    val efectivoEsperado: BigDecimal,

    @ColumnInfo(name = "efectivo_fisico_real")
    val efectivoFisicoReal: BigDecimal,

    @ColumnInfo(name = "diferencia")
    val diferencia: BigDecimal,

    @ColumnInfo(name = "notas")
    val notas: String? = null
)

@Entity(tableName = "sesiones_caja")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "moneda")
    val moneda: CurrencyType = CurrencyType.NIO,

    @ColumnInfo(name = "saldo_inicial")
    val saldoInicial: BigDecimal,

    @ColumnInfo(name = "saldo_actual")
    val saldoActual: BigDecimal,

    @ColumnInfo(name = "fecha_apertura")
    val fechaApertura: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fecha_cierre")
    val fechaCierre: Long? = null,

    @ColumnInfo(name = "estado")
    val estado: String = "ABIERTA",

    @ColumnInfo(name = "apertura_automatica")
    val aperturaAutomatica: Boolean = false,

    @ColumnInfo(name = "notas")
    val notas: String? = null
)

@Entity(
    tableName = "desembolsos",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["prestamo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prestamo_id"])]
)
data class DisbursementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "monto_desembolsado")
    val montoDesembolsado: BigDecimal,

    @ColumnInfo(name = "fecha_programada")
    val fechaProgramada: Long,

    @ColumnInfo(name = "fecha_desembolso_real")
    val fechaDesembolsoReal: Long? = null,

    @ColumnInfo(name = "metodo_desembolso")
    val metodoDesembolso: PaymentMethod = PaymentMethod.CASH,

    @ColumnInfo(name = "estado")
    val estado: String = "PROGRAMADO",

    @ColumnInfo(name = "comprobante_uri")
    val comprobanteUri: String? = null,

    @ColumnInfo(name = "notas")
    val notas: String? = null
)

@Entity(tableName = "movimientos_caja")
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 1L,

    @ColumnInfo(name = "moneda")
    val moneda: CurrencyType = CurrencyType.NIO,

    @ColumnInfo(name = "tipo_movimiento")
    val tipoMovimiento: String, // "INGRESO", "EGRESO", "AJUSTE"

    @ColumnInfo(name = "monto")
    val monto: BigDecimal,

    @ColumnInfo(name = "concepto")
    val concepto: String,

    @ColumnInfo(name = "categoria")
    val categoria: String? = null,

    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long? = null,

    @ColumnInfo(name = "fecha_movimiento")
    val fechaMovimiento: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "garantias",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["prestamo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prestamo_id"])]
)
data class GuaranteeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long,
    
    @ColumnInfo(name = "tipo_garantia")
    val tipoGarantia: String, // "Vehículo", "Muebles", "Equipos Tecnológicos"
    
    @ColumnInfo(name = "valor_estimado")
    val valorEstimado: BigDecimal,
    
    @ColumnInfo(name = "estado_bien")
    val estadoBien: String, // "Nuevo", "Usado", "En Buen Estado"
    
    @ColumnInfo(name = "descripcion")
    val descripcion: String,
    
    @ColumnInfo(name = "foto_principal_uri")
    val fotoPrincipalUri: String? = null,
    
    @ColumnInfo(name = "foto_circulacion_uri")
    val fotoCirculacionUri: String? = null // Específico para vehículos
)

@Entity(tableName = "origenes_capital")
data class CapitalOriginEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "nombre")
    val nombre: String,
    
    @ColumnInfo(name = "descripcion")
    val descripcion: String? = null,
    
    @ColumnInfo(name = "saldo_actual")
    val saldoActual: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "empresa_id")
    val empresaId: Long = 0L,
    
    @ColumnInfo(name = "activo")
    val activo: Boolean = true,
    
    @ColumnInfo(name = "total_invertido")
    val totalInvertido: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "total_retornado")
    val totalRetornado: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transacciones_capital",
    foreignKeys = [
        ForeignKey(
            entity = CapitalOriginEntity::class,
            parentColumns = ["id"],
            childColumns = ["origen_capital_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["origen_capital_id"])]
)
data class CapitalTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "origen_capital_id")
    val origenCapitalId: Long,
    
    @ColumnInfo(name = "empresa_id")
    val empresaId: Long? = null,
    
    @ColumnInfo(name = "tipo")
    val tipo: String, // "INGRESO", "EGRESO"
    
    @ColumnInfo(name = "monto")
    val monto: BigDecimal,
    
    @ColumnInfo(name = "fecha")
    val fecha: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "notas")
    val notas: String? = null
)

@Entity(
    tableName = "prestamo_origenes_capital",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["prestamo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CapitalOriginEntity::class,
            parentColumns = ["id"],
            childColumns = ["origen_capital_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prestamo_id"]), Index(value = ["origen_capital_id"])]
)
data class LoanCapitalSourceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "prestamo_id")
    val prestamoId: Long,
    
    @ColumnInfo(name = "origen_capital_id")
    val origenCapitalId: Long,
    
    @ColumnInfo(name = "monto_aportado")
    val montoAportado: BigDecimal
)

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "nombre_completo")
    val nombreCompleto: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "telefono")
    val telefono: String = "",

    @ColumnInfo(name = "rol")
    val rol: String = "Administrador",

    @ColumnInfo(name = "pin")
    val pin: String = "1234",

    @ColumnInfo(name = "foto_uri")
    val fotoUri: String? = null,

    @ColumnInfo(name = "biometria_habilitada")
    val biometriaHabilitada: Boolean = false,

    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)

@Entity(tableName = "info_empresas")
data class CompanyInfoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long, // 1 = Creciendo Juntos, 2 = Facilito

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "telefono")
    val telefono: String,

    @ColumnInfo(name = "direccion")
    val direccion: String = "Nicaragua",

    @ColumnInfo(name = "ruc")
    val ruc: String = "",

    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "tasa_interes_defecto")
    val tasaInteresDefecto: BigDecimal = BigDecimal("10.00")
)

