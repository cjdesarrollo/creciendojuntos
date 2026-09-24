package com.prestamos.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prestamos.app.ui.screens.auth.LockScreen
import com.prestamos.app.ui.screens.backup.BackupScreen
import com.prestamos.app.ui.screens.cashaudit.CashAuditScreen
import com.prestamos.app.ui.screens.clients.ClientFormScreen
import com.prestamos.app.ui.screens.clients.ClientListScreen
import com.prestamos.app.ui.screens.dashboard.DashboardScreen
import com.prestamos.app.ui.screens.loans.AmortizationSimulationScreen
import com.prestamos.app.ui.screens.loans.CreateLoanScreen
import com.prestamos.app.ui.screens.loans.LoanDetailScreen
import com.prestamos.app.ui.screens.loans.LoanListScreen
import com.prestamos.app.ui.screens.payments.PaymentDashboardScreen
import com.prestamos.app.ui.screens.payments.PaymentScreen
import com.prestamos.app.ui.theme.AccentYellowDark
import com.prestamos.app.ui.theme.LightSurface
import com.prestamos.app.ui.theme.PrimaryBlue
import com.prestamos.app.ui.theme.RubyRed
import com.prestamos.app.ui.theme.SecondaryGreen
import com.prestamos.app.ui.theme.TextPrimary
import com.prestamos.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class DrawerMenuItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Lock : Screen("lock")
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Loans : Screen("loans", "Préstamos", Icons.Default.AccountBalanceWallet)
    object Clients : Screen("clients", "Clientes", Icons.Default.People)
    object CashAudit : Screen("cash_audit", "Caja", Icons.Default.PointOfSale)
    object Simulate : Screen("simulate", "Simulador", Icons.Default.Calculate)
    object Backup : Screen("backup", "Copia de Seguridad", Icons.Default.Backup)
    object CapitalOrigin : Screen("capital_origin", "Origen de Capital", Icons.Default.AccountBalance)
    object Settings : Screen("settings", "Configuración", Icons.Default.Settings)
    object AddClient : Screen("add_client")
    object CreateLoan : Screen("create_loan")
    object PaymentDashboard : Screen("payment_dashboard")
    object CapitalOriginDetail : Screen("capital_origin_detail/{originId}") {
        fun createRoute(originId: Long) = "capital_origin_detail/$originId"
    }
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object Payment : Screen("payment/{loanId}") {
        fun createRoute(loanId: Long) = "payment/$loanId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: com.prestamos.app.ui.viewmodel.AuthViewModel,
    dashboardViewModel: com.prestamos.app.ui.viewmodel.DashboardViewModel,
    clientViewModel: com.prestamos.app.ui.viewmodel.ClientViewModel,
    loanViewModel: com.prestamos.app.ui.viewmodel.LoanViewModel,
    paymentViewModel: com.prestamos.app.ui.viewmodel.PaymentViewModel,
    cashAuditViewModel: com.prestamos.app.ui.viewmodel.CashAuditViewModel,
    backupViewModel: com.prestamos.app.ui.viewmodel.BackupViewModel,
    capitalOriginViewModel: com.prestamos.app.ui.viewmodel.CapitalOriginViewModel,
    settingsViewModel: com.prestamos.app.ui.viewmodel.SettingsViewModel,
    onBiometricPromptRequest: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val startDestination = if (authState.isAuthenticated) Screen.Home.route else Screen.Lock.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val drawerItems = listOf(
        DrawerMenuItem(Screen.Home, "Inicio / Dashboard", Icons.Default.Home, PrimaryBlue),
        DrawerMenuItem(Screen.Loans, "Préstamos", Icons.Default.AccountBalanceWallet, SecondaryGreen),
        DrawerMenuItem(Screen.Simulate, "  └ Simulador de Crédito", Icons.Default.Calculate, SecondaryGreen),
        DrawerMenuItem(Screen.Clients, "Clientes", Icons.Default.People, SecondaryGreen),
        DrawerMenuItem(Screen.PaymentDashboard, "Pagos", Icons.Default.PointOfSale, SecondaryGreen),
        DrawerMenuItem(Screen.CashAudit, "Caja & Movimientos", Icons.Default.PointOfSale, AccentYellowDark),
        DrawerMenuItem(Screen.Settings, "Configuración", Icons.Default.Settings, PrimaryBlue),
        DrawerMenuItem(Screen.Backup, "Copia de Seguridad", Icons.Default.Backup, TextSecondary)
    )

    val isDrawerEnabled = authState.isAuthenticated && currentRoute != Screen.Lock.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerEnabled,
        drawerContent = {
            if (isDrawerEnabled) {
                ModalDrawerSheet(
                    drawerContainerColor = LightSurface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Cabecera Moderna del Menú Lateral
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryBlue)
                                .padding(20.dp)
                        ) {
                            Column {
                                Surface(
                                    modifier = Modifier.size(52.dp).clip(CircleShape),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("CJ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Creciendo Juntos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("Sistema de Préstamos v2.0", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Elementos de Menú
                        drawerItems.forEach { item ->
                            val selected = currentRoute == item.screen.route
                            NavigationDrawerItem(
                                icon = { Icon(imageVector = item.icon, contentDescription = item.title, tint = if (selected) item.color else TextSecondary) },
                                label = { Text(item.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) item.color else TextPrimary) },
                                selected = selected,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    if (currentRoute != item.screen.route) {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = item.color.copy(alpha = 0.12f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Bloquear App
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Bloquear", tint = RubyRed) },
                            label = { Text("Bloquear / Cerrar Sesión", fontWeight = FontWeight.Bold, color = RubyRed) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                authViewModel.lockApp()
                                navController.navigate(Screen.Lock.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    ) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Lock.route) {
                    LockScreen(
                        viewModel = authViewModel,
                        onBiometricPromptRequest = onBiometricPromptRequest
                    )
                    if (authState.isAuthenticated) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                }

                composable(Screen.Home.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToNewLoan = { navController.navigate(Screen.CreateLoan.route) },
                        onNavigateToClients = { navController.navigate(Screen.Clients.route) },
                        onNavigateToCashAudit = { navController.navigate(Screen.CashAudit.route) },
                        onNavigateToLoanDetail = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) }
                    )
                }

                composable(Screen.Loans.route) {
                    LoanListScreen(
                        viewModel = loanViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToCreateLoan = { navController.navigate(Screen.CreateLoan.route) },
                        onNavigateToSimulate = { navController.navigate(Screen.Simulate.route) },
                        onNavigateToPayment = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) },
                        onBack = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Clients.route) {
                    ClientListScreen(
                        viewModel = clientViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToAddClient = { navController.navigate(Screen.AddClient.route) },
                        onNavigateToLoanDetail = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) },
                        onNavigateToRefinance = { clientId, loanId ->
                            if (loanId != null && loanId > 0) {
                                navController.navigate(Screen.LoanDetail.createRoute(loanId))
                            } else {
                                loanViewModel.onClientSelected(clientId)
                                navController.navigate(Screen.CreateLoan.route)
                            }
                        },
                        onBack = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.PaymentDashboard.route) {
                    PaymentDashboardScreen(
                        viewModel = loanViewModel,
                        paymentViewModel = paymentViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToPayment = { loanId -> navController.navigate(Screen.Payment.createRoute(loanId)) }
                    )
                }

                composable(Screen.CashAudit.route) {
                    CashAuditScreen(
                        viewModel = cashAuditViewModel,
                        loanViewModel = loanViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToLoanPayment = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) }
                    )
                }

                composable(Screen.CapitalOrigin.route) {
                    com.prestamos.app.ui.screens.capital.CapitalOriginScreen(
                        viewModel = capitalOriginViewModel,
                        onNavigateBack = { coroutineScope.launch { drawerState.open() } },
                        onNavigateToDetail = { originId -> navController.navigate(Screen.CapitalOriginDetail.createRoute(originId)) }
                    )
                }

                composable(
                    route = Screen.CapitalOriginDetail.route,
                    arguments = listOf(navArgument("originId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val originId = backStackEntry.arguments?.getLong("originId") ?: 0L
                    com.prestamos.app.ui.screens.capital.CapitalOriginDetailScreen(
                        originId = originId,
                        viewModel = capitalOriginViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Simulate.route) {
                    AmortizationSimulationScreen(
                        viewModel = loanViewModel,
                        onNavigateToCreateLoan = { navController.navigate(Screen.CreateLoan.route) },
                        onBack = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.AddClient.route) {
                    ClientFormScreen(
                        viewModel = clientViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.CreateLoan.route) {
                    CreateLoanScreen(
                        viewModel = loanViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.LoanDetail.route,
                    arguments = listOf(navArgument("loanId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                    LoanDetailScreen(
                        loanId = loanId,
                        viewModel = paymentViewModel,
                        onNavigateToPayment = { id -> navController.navigate(Screen.Payment.createRoute(id)) },
                        onNavigateToRefinance = { clientId -> 
                            loanViewModel.onClientSelected(clientId)
                            navController.navigate(Screen.CreateLoan.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Payment.route,
                    arguments = listOf(navArgument("loanId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                    PaymentScreen(
                        loanId = loanId,
                        viewModel = paymentViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    com.prestamos.app.ui.screens.settings.SettingsScreen(
                        viewModel = settingsViewModel,
                        capitalOriginViewModel = capitalOriginViewModel,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Backup.route) {
                    BackupScreen(
                        viewModel = backupViewModel,
                        onBack = { coroutineScope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
