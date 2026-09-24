package com.prestamos.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.prestamos.app.ui.navigation.AppNavigation
import com.prestamos.app.ui.theme.PrestamoTheme
import com.prestamos.app.ui.viewmodel.AuthViewModel
import com.prestamos.app.ui.viewmodel.BackupViewModel
import com.prestamos.app.ui.viewmodel.CashAuditViewModel
import com.prestamos.app.ui.viewmodel.ClientViewModel
import com.prestamos.app.ui.viewmodel.DashboardViewModel
import com.prestamos.app.ui.viewmodel.LoanViewModel
import com.prestamos.app.ui.viewmodel.PaymentViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PrestamoApp

        val authViewModel = AuthViewModel(app.securityManager, app.biometricAuthenticator)
        val dashboardViewModel = DashboardViewModel(app.repository, app.excelImporterService)
        val clientViewModel = ClientViewModel(app.repository, app.fileStorageManager)
        val loanViewModel = LoanViewModel(app.repository)
        val paymentViewModel = PaymentViewModel(
            app.repository,
            app.bluetoothPrinterManager,
            app.pdfReceiptGenerator,
            app.fileStorageManager
        )
        val cashAuditViewModel = CashAuditViewModel(app.repository)
        val backupViewModel = BackupViewModel(this, app.repository, app.excelCsvExporter)
        val capitalOriginViewModel = com.prestamos.app.ui.viewmodel.CapitalOriginViewModel(app.capitalOriginRepository)
        val settingsViewModel = com.prestamos.app.ui.viewmodel.SettingsViewModel(app.configRepository)

        setContent {
            PrestamoTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    clientViewModel = clientViewModel,
                    loanViewModel = loanViewModel,
                    paymentViewModel = paymentViewModel,
                    cashAuditViewModel = cashAuditViewModel,
                    backupViewModel = backupViewModel,
                    capitalOriginViewModel = capitalOriginViewModel,
                    settingsViewModel = settingsViewModel,
                    onBiometricPromptRequest = {
                        app.biometricAuthenticator.promptBiometric(
                            activity = this,
                            onSuccess = { authViewModel.onBiometricSuccess() },
                            onError = { /* Error handled in VM */ }
                        )
                    }
                )
            }
        }
    }
}
