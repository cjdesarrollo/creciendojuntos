package com.prestamos.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prestamos.app.data.local.db.dao.CashAuditDao
import com.prestamos.app.data.local.db.dao.CashMovementDao
import com.prestamos.app.data.local.db.dao.CashSessionDao
import com.prestamos.app.data.local.db.dao.ClientDao
import com.prestamos.app.data.local.db.dao.DisbursementDao
import com.prestamos.app.data.local.db.dao.LoanDao
import com.prestamos.app.data.local.db.dao.PaymentDao
import com.prestamos.app.data.local.db.entities.CashAuditEntity
import com.prestamos.app.data.local.db.entities.CashMovementEntity
import com.prestamos.app.data.local.db.entities.CashSessionEntity
import com.prestamos.app.data.local.db.entities.ClientEntity
import com.prestamos.app.data.local.db.entities.DisbursementEntity
import com.prestamos.app.data.local.db.entities.InstallmentEntity
import com.prestamos.app.data.local.db.entities.LoanEntity
import com.prestamos.app.data.local.db.entities.PaymentEntity
import com.prestamos.app.data.local.db.entities.GuaranteeEntity
import com.prestamos.app.data.local.db.entities.CapitalOriginEntity
import com.prestamos.app.data.local.db.entities.CapitalTransactionEntity
import com.prestamos.app.data.local.db.entities.LoanCapitalSourceEntity
import com.prestamos.app.data.local.db.entities.UserEntity
import com.prestamos.app.data.local.db.entities.CompanyInfoEntity
import com.prestamos.app.data.local.db.dao.GuaranteeDao
import com.prestamos.app.data.local.db.dao.CapitalOriginDao
import com.prestamos.app.data.local.db.dao.CapitalTransactionDao
import com.prestamos.app.data.local.db.dao.LoanCapitalSourceDao
import com.prestamos.app.data.local.db.dao.UserDao
import com.prestamos.app.data.local.db.dao.CompanyInfoDao
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ClientEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        PaymentEntity::class,
        CashAuditEntity::class,
        CashSessionEntity::class,
        DisbursementEntity::class,
        CashMovementEntity::class,
        GuaranteeEntity::class,
        CapitalOriginEntity::class,
        CapitalTransactionEntity::class,
        LoanCapitalSourceEntity::class,
        UserEntity::class,
        CompanyInfoEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cashAuditDao(): CashAuditDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun disbursementDao(): DisbursementDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun guaranteeDao(): GuaranteeDao
    abstract fun capitalOriginDao(): CapitalOriginDao
    abstract fun capitalTransactionDao(): CapitalTransactionDao
    abstract fun loanCapitalSourceDao(): LoanCapitalSourceDao
    abstract fun userDao(): UserDao
    abstract fun companyInfoDao(): CompanyInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clientes ADD COLUMN es_incobrable INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clientes ADD COLUMN en_recuperacion INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                SQLiteDatabase.loadLibs(context.applicationContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prestamos_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
