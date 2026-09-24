package com.prestamos.app.ui.screens.settings

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prestamos.app.domain.model.AppUser
import com.prestamos.app.domain.model.CapitalOrigin
import com.prestamos.app.domain.model.Company
import com.prestamos.app.domain.model.CompanyProfile
import com.prestamos.app.ui.theme.*
import com.prestamos.app.ui.viewmodel.CapitalOriginViewModel
import com.prestamos.app.ui.viewmodel.SettingsViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    capitalOriginViewModel: CapitalOriginViewModel? = null,
    onMenuClick: () -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    val users by viewModel.users.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val origins by (capitalOriginViewModel?.origins?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val capitalStatusMessage by (capitalOriginViewModel?.statusMessage?.collectAsState() ?: remember { mutableStateOf(null) })

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Empresas", "Usuarios", "Orígenes de Capital")

    var editingCompany by remember { mutableStateOf<CompanyProfile?>(null) }
    var editingUser by remember { mutableStateOf<AppUser?>(null) }
    var isCreatingUser by remember { mutableStateOf(false) }

    var isCreatingOrigin by remember { mutableStateOf(false) }
    var editingOrigin by remember { mutableStateOf<CapitalOrigin?>(null) }
    var originToIncreaseCapital by remember { mutableStateOf<CapitalOrigin?>(null) }
    var originToDelete by remember { mutableStateOf<CapitalOrigin?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(capitalStatusMessage) {
        capitalStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            capitalOriginViewModel?.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración General", fontWeight = FontWeight.Bold, color = NavyPrimary) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = NavyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { isCreatingUser = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo Usuario")
                }
            } else if (selectedTabIndex == 2) {
                FloatingActionButton(
                    onClick = { isCreatingOrigin = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Origen de Capital")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightSurface)
                .padding(innerPadding)
        ) {
            // Pestañas de Navegación
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CardSurface,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Business
                                        1 -> Icons.Default.People
                                        else -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(title, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // PESTAÑA EMPRESAS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Información de las Empresas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Configura los teléfonos oficiales, direcciones y datos que aparecen en recibos y contratos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        items(companies) { company ->
                            CompanyCard(
                                company = company,
                                onEditClick = { editingCompany = company }
                            )
                        }
                    }
                }
                1 -> {
                    // PESTAÑA USUARIOS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Usuarios y Accesos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Gestiona administradores, cobradores, fotos y huella dactilar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        items(users) { user ->
                            UserCard(
                                user = user,
                                onEditClick = { editingUser = user },
                                onDeleteClick = { viewModel.deleteUser(user) },
                                onToggleBiometric = { enabled -> viewModel.toggleUserBiometric(user.id, enabled) }
                            )
                        }
                    }
                }
                2 -> {
                    // PESTAÑA ORÍGENES DE CAPITAL
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Orígenes y Fondos de Capital",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Administra las fuentes de financiamiento, edita parámetros e inyecta o aumenta capital.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { isCreatingOrigin = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nuevo", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (origins.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No hay orígenes de capital registrados. Toca '+ Nuevo' para crear uno.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        } else {
                            items(origins) { origin ->
                                CapitalOriginSettingCard(
                                    origin = origin,
                                    onEditClick = { editingOrigin = origin },
                                    onIncreaseCapitalClick = { originToIncreaseCapital = origin },
                                    onDeleteClick = { originToDelete = origin }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO EDITAR EMPRESA
    editingCompany?.let { comp ->
        EditCompanyDialog(
            company = comp,
            onDismiss = { editingCompany = null },
            onSave = { updatedComp ->
                viewModel.saveCompany(updatedComp)
                editingCompany = null
            }
        )
    }

    // DIÁLOGOS DE ORÍGENES DE CAPITAL
    if (isCreatingOrigin) {
        CapitalOriginFormDialog(
            origin = null,
            onDismiss = { isCreatingOrigin = false },
            onSave = { name, desc, companyId, initialBalance ->
                capitalOriginViewModel?.createOrigin(name, desc, companyId, initialBalance) {
                    isCreatingOrigin = false
                }
            }
        )
    }

    editingOrigin?.let { origin ->
        CapitalOriginFormDialog(
            origin = origin,
            onDismiss = { editingOrigin = null },
            onSave = { name, desc, companyId, _ ->
                capitalOriginViewModel?.updateOrigin(origin.copy(name = name, description = desc, companyId = companyId)) {
                    editingOrigin = null
                }
            }
        )
    }

    originToIncreaseCapital?.let { origin ->
        IncreaseCapitalDialog(
            origin = origin,
            onDismiss = { originToIncreaseCapital = null },
            onConfirm = { amount, notes ->
                capitalOriginViewModel?.increaseCapital(origin.id, amount, notes) {
                    originToIncreaseCapital = null
                }
            }
        )
    }

    // DIÁLOGO CREAR / EDITAR USUARIO
    if (isCreatingUser || editingUser != null) {
        val userToEdit = editingUser ?: AppUser(
            fullName = "",
            username = "",
            phone = "",
            role = "Cobrador",
            pin = "1234",
            photoUri = null,
            biometricEnabled = false
        )

        UserFormDialog(
            user = userToEdit,
            isNew = isCreatingUser,
            onDismiss = {
                isCreatingUser = false
                editingUser = null
            },
            onSave = { user ->
                viewModel.saveUser(user) {
                    isCreatingUser = false
                    editingUser = null
                }
            }
        )
    }

    // DIÁLOGO CONFIRMAR ELIMINACIÓN DE ORIGEN DE CAPITAL
    originToDelete?.let { origin ->
        AlertDialog(
            onDismissRequest = { originToDelete = null },
            title = { Text("Eliminar Origen de Capital", fontWeight = FontWeight.Bold, color = RubyRed) },
            text = {
                Text("¿Estás seguro de que deseas eliminar el origen '${origin.name}'? Esta acción eliminará el registro y sus transacciones de capital asociadas.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        capitalOriginViewModel?.deleteOrigin(origin.id)
                        originToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { originToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProfilePhotoView(
    photoUriString: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(photoUriString) {
        if (photoUriString.isNullOrBlank()) null
        else {
            try {
                val uri = Uri.parse(photoUriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto de perfil",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = NavyPrimary,
            modifier = modifier.padding(10.dp)
        )
    }
}

@Composable
fun CompanyCard(
    company: CompanyProfile,
    onEditClick: () -> Unit
) {
    val companyEnum = Company.values().find { it.id == company.id } ?: Company.CRECIENDO_JUNTOS
    val isCj = company.id == 1L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Logo Container
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        if (companyEnum.iconRes != null) {
                            Image(
                                painter = painterResource(id = companyEnum.iconRes),
                                contentDescription = company.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = company.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCj) NavyPrimary else PrimaryBlue
                        )
                        Text(
                            text = if (isCj) "Microcréditos Comunitarios" else "Créditos Rápidos y Flexibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryBlue)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Teléfono Oficial:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(
                        text = company.phone.ifBlank { companyEnum.defaultPhone },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentYellowDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ubicación:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(
                        text = company.address,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "RUC: ${company.ruc.ifBlank { "Sin RUC registrado" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Tasa base: ${company.defaultInterestRate}% mensual",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCj) AccentYellowDark else PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun UserCard(
    user: AppUser,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Avatar / Foto
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfilePhotoView(
                            photoUriString = user.photoUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = when (user.role) {
                                    "Administrador" -> NavyPrimary.copy(alpha = 0.15f)
                                    "Supervisor" -> AccentYellowDark.copy(alpha = 0.15f)
                                    else -> EmeraldGreen.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.role,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (user.role) {
                                        "Administrador" -> NavyPrimary
                                        "Supervisor" -> AccentYellowDark
                                        else -> EmeraldGreen
                                    }
                                )
                            }
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryBlue)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RubyRed)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (user.biometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (user.biometricEnabled) EmeraldGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (user.biometricEnabled) "Huella Vinculada ✓" else "Huella Desactivada",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.biometricEnabled) EmeraldGreen else TextSecondary
                    )
                }

                Switch(
                    checked = user.biometricEnabled,
                    onCheckedChange = onToggleBiometric,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldGreen,
                        checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
fun EditCompanyDialog(
    company: CompanyProfile,
    onDismiss: () -> Unit,
    onSave: (CompanyProfile) -> Unit
) {
    var name by remember { mutableStateOf(company.name) }
    var phone by remember { mutableStateOf(company.phone) }
    var address by remember { mutableStateOf(company.address) }
    var ruc by remember { mutableStateOf(company.ruc) }
    var email by remember { mutableStateOf(company.email) }
    var rate by remember { mutableStateOf(company.defaultInterestRate.toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Información de Empresa", fontWeight = FontWeight.Bold, color = NavyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de Empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono Oficial de Contacto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ruc,
                    onValueChange = { ruc = it },
                    label = { Text("RUC / Identificación Fiscal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Tasa de Interés Sugerida (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rateBD = rate.toBigDecimalOrNull() ?: company.defaultInterestRate
                    onSave(
                        company.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            ruc = ruc.trim(),
                            email = email.trim(),
                            defaultInterestRate = rateBD
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun UserFormDialog(
    user: AppUser,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppUser) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var username by remember { mutableStateOf(user.username) }
    var phone by remember { mutableStateOf(user.phone) }
    var role by remember { mutableStateOf(user.role) }
    var pin by remember { mutableStateOf(user.pin) }
    var photoUri by remember { mutableStateOf(user.photoUri) }
    var biometricEnabled by remember { mutableStateOf(user.biometricEnabled) }

    val roles = listOf("Administrador", "Cobrador", "Supervisor")

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        photoUri = uri?.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Nuevo Usuario" else "Editar Usuario",
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Foto
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.1f))
                                .clickable { photoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfilePhotoView(
                                photoUriString = photoUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        TextButton(onClick = { photoLauncher.launch("image/*") }) {
                            Text(if (photoUri != null) "Cambiar Foto" else "Agregar Foto de Perfil")
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de Usuario (Login) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN de Seguridad (4 Dígitos) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rol del Usuario *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    roles.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { role = r }
                        ) {
                            RadioButton(selected = role == r, onClick = { role = r })
                            Text(r, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vincular Huella Dactilar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Permite iniciar sesión con biometría", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && username.isNotBlank()) {
                        onSave(
                            user.copy(
                                fullName = fullName.trim(),
                                username = username.trim(),
                                phone = phone.trim(),
                                role = role,
                                pin = pin.trim(),
                                photoUri = photoUri,
                                biometricEnabled = biometricEnabled
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(if (isNew) "Crear Usuario" else "Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CapitalOriginSettingCard(
    origin: CapitalOrigin,
    onEditClick: () -> Unit,
    onIncreaseCapitalClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val companyName = when (origin.companyId) {
        1L -> "Creciendo Juntos"
        2L -> "Facilito"
        else -> "Todas las Empresas"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Nombre, Empresa y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = origin.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = companyName,
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (origin.isActive) EmeraldGreen.copy(alpha = 0.12f) else RubyRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (origin.isActive) "Activo" else "Inactivo",
                        color = if (origin.isActive) EmeraldGreen else RubyRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!origin.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = origin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta de Saldos
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LightSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saldo Disponible:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "C$ ${origin.currentBalance}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderSlate.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Invertido", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("C$ ${origin.totalInvested}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = NavyPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Retornado", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("C$ ${origin.totalReturned}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onIncreaseCapitalClick,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aumentar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar Origen",
                        tint = RubyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CapitalOriginFormDialog(
    origin: CapitalOrigin?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, companyId: Long, initialBalance: BigDecimal) -> Unit
) {
    val isNew = origin == null
    var name by remember { mutableStateOf(origin?.name ?: "") }
    var description by remember { mutableStateOf(origin?.description ?: "") }
    var companyId by remember { mutableStateOf(origin?.companyId ?: 0L) }
    var initialBalanceText by remember { mutableStateOf(if (isNew) "" else origin!!.currentBalance.toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Nuevo Origen de Capital" else "Editar Origen de Capital",
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Origen / Inversionista *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o Referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text("Empresa Asignada *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                val companyOptions = listOf(
                    0L to "Todas las Empresas",
                    1L to "Creciendo Juntos",
                    2L to "Facilito"
                )
                Column {
                    companyOptions.forEach { (id, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { companyId = id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = companyId == id, onClick = { companyId = id })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (isNew) {
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { initialBalanceText = it },
                        label = { Text("Saldo Inicial (C$, Opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initBal = initialBalanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        onSave(name.trim(), description.trim().ifEmpty { null }, companyId, initBal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(if (isNew) "Crear Origen" else "Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun IncreaseCapitalDialog(
    origin: CapitalOrigin,
    onDismiss: () -> Unit,
    onConfirm: (amount: BigDecimal, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aumentar Capital", fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(origin.name, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saldo actual: C$ ${origin.currentBalance}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto a Inyectar / Aumentar (C$) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notas / Referencia (ej. Depósito BAC, Efectivo)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull()
                    if (amount != null && amount > BigDecimal.ZERO) {
                        onConfirm(amount, notesText.trim().ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Confirmar Aumento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
