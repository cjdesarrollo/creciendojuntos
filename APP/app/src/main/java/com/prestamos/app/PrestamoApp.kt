package com.prestamos.app

import android.app.Application
import com.prestamos.app.data.export.ExcelCsvExporter
import com.prestamos.app.data.export.PdfReceiptGenerator
import com.prestamos.app.data.hardware.BluetoothPrinterManager
import com.prestamos.app.data.local.db.AppDatabase
import com.prestamos.app.data.local.db.DatabaseSeeder
import com.prestamos.app.data.local.security.BiometricAuthenticator
import com.prestamos.app.data.local.security.SecurityManager
import com.prestamos.app.data.local.storage.FileStorageManager
import com.prestamos.app.data.repository.LoanRepository
import com.prestamos.app.data.importer.ExcelImporterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrestamoApp : Application() {

    lateinit var securityManager: SecurityManager
        private set
    lateinit var biometricAuthenticator: BiometricAuthenticator
        private set
    lateinit var fileStorageManager: FileStorageManager
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var repository: LoanRepository
        private set
    lateinit var bluetoothPrinterManager: BluetoothPrinterManager
        private set
    lateinit var pdfReceiptGenerator: PdfReceiptGenerator
        private set
    lateinit var excelCsvExporter: ExcelCsvExporter
        private set
    lateinit var excelImporterService: ExcelImporterService
        private set

    lateinit var capitalOriginRepository: com.prestamos.app.data.repository.CapitalOriginRepository
        private set
    lateinit var configRepository: com.prestamos.app.data.repository.ConfigRepository
        private set

    override fun onCreate() {
        super.onCreate()

        securityManager = SecurityManager(this)
        biometricAuthenticator = BiometricAuthenticator(this)
        fileStorageManager = FileStorageManager(this)

        val passphrase = securityManager.getOrCreateDatabasePassphrase()
        database = AppDatabase.getInstance(this, passphrase)
        repository = LoanRepository(database)
        capitalOriginRepository = com.prestamos.app.data.repository.CapitalOriginRepository(database.capitalOriginDao(), database.capitalTransactionDao())
        configRepository = com.prestamos.app.data.repository.ConfigRepository(database.userDao(), database.companyInfoDao())

        bluetoothPrinterManager = BluetoothPrinterManager(this)
        pdfReceiptGenerator = PdfReceiptGenerator(this, fileStorageManager)
        excelCsvExporter = ExcelCsvExporter(this, fileStorageManager)
        excelImporterService = ExcelImporterService(this, database.clientDao(), database.loanDao())

        // Cargar datos de prueba automáticamente si la base de datos está vacía
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val seeder = DatabaseSeeder(repository, configRepository)
                seeder.seedSampleDataIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
