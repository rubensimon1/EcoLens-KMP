package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.HistoryManager
import com.rubensimon.ecolens.utils.PointsManager
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign

/**
 * Pantalla de ajustes — migrada de SettingsActivity.
 *
 * Controla: tema (dark/light), audio, URL del backend, datos de cuenta.
 * Usa multiplatform-settings en lugar de SharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings = remember { Settings() }

    // Estado del tema
    var isDarkMode by remember { mutableStateOf(EcoColors.isDark) }
    // Audio
    var musicEnabled by remember { mutableStateOf(settings.getBoolean("audio_music_enabled", true)) }
    var effectsEnabled by remember { mutableStateOf(settings.getBoolean("audio_effects_enabled", true)) }
    // Notificaciones
    var pushEnabled by remember { mutableStateOf(settings.getBoolean("notify_push", true)) }
    var rewardAlerts by remember { mutableStateOf(settings.getBoolean("notify_rewards", true)) }
    var emailAlerts by remember { mutableStateOf(settings.getBoolean("notify_email", false)) }

    // Cuenta
    val username = remember { settings.getString("username", "Usuario") }
    val email = remember { settings.getString("email", "") }
    val userId = remember { settings.getString("user_id", "") }

    // Diálogos
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var infoTitle by remember { mutableStateOf("") }
    var infoText by remember { mutableStateOf("") }
    var currentDisplayName by remember { mutableStateOf(username) }
    var currentEmail by remember { mutableStateOf(email) }
    var currentBio by remember { mutableStateOf("") }

    var newDisplayName by remember { mutableStateOf("") }
    var newBio by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatNewPassword by remember { mutableStateOf("") }

    // Estado de mensajes
    var statusMessage by remember { mutableStateOf("") }
    var isErrorStatus by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val repo = UserRepository()
            // Recuperar email real de la sesión
            repo.getCurrentUserEmail()?.let { 
                currentEmail = it 
            }
            
            val user = repo.getUserById(userId)
            user?.let {
                currentDisplayName = it.display_name ?: it.username
                currentBio = it.bio ?: ""
                newDisplayName = currentDisplayName
                newBio = currentBio
                
                // ── Sincronización Inicial: solo si el local está vacío o es la primera vez ──
                if (!settings.hasKey("sync_done")) {
                    pushEnabled = it.notify_push
                    rewardAlerts = it.notify_rewards
                    emailAlerts = it.notify_email
                    settings.putBoolean("notify_push", it.notify_push)
                    settings.putBoolean("notify_rewards", it.notify_rewards)
                    settings.putBoolean("notify_email", it.notify_email)
                    settings.putBoolean("sync_done", true)
                }
            }
        }
    }

    // Diálogo de información (Privacidad, Soporte, etc)
    if (showInfoDialog) {
        ModernDialog(
            onDismissRequest = { showInfoDialog = false },
            title = infoTitle,
            confirmButtonText = "Entendido",
            onConfirm = { showInfoDialog = false }
        ) {
            Text(
                infoText,
                color = EcoColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }

    // ── Confirmación borrar datos ─────────────────────────────────────────
    if (showClearDataDialog) {
        ModernDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = "⚠️ Borrar datos locales",
            confirmButtonText = "Borrar Todo",
            confirmColor = EcoColors.Error,
            onConfirm = {
                PointsManager.reset()
                HistoryManager.clearHistory()
                settings.remove("collection_unlocked")
                showClearDataDialog = false
                statusMessage = "✅ Caché y datos locales borrados"
            }
        ) {
            Text(
                "Se eliminarán tus puntos, historial y colección local del dispositivo. Esta acción no se puede deshacer.", 
                color = EcoColors.TextSecondary, 
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }

    // ── Confirmación eliminar cuenta ──────────────────────────────────────
    if (showDeleteAccountDialog) {
        ModernDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = "🔴 Eliminar cuenta",
            confirmButtonText = "Eliminar",
            confirmColor = EcoColors.Error,
            onConfirm = {
                showDeleteAccountDialog = false
                statusMessage = "⚠️ Solicitud de eliminación enviada"
            }
        ) {
            Text(
                "Esta acción es irreversible. Se perderán todos tus logros, puntos y datos en la nube para siempre.",
                color = EcoColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }

    // ── Cambio de Email ───────────────────────────────────────────────────
    if (showChangeEmailDialog) {
        ModernDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            title = "✉️ Cambiar Email",
            confirmButtonText = "Actualizar",
            onConfirm = {
                scope.launch {
                    val success = UserRepository().updateUserEmail(newEmail)
                    if (success) {
                        statusMessage = "📩 Confirma el cambio en tu nuevo email: $newEmail"
                        isErrorStatus = false
                        showChangeEmailDialog = false
                    } else {
                        statusMessage = "❌ Error: Verifica el formato del email"
                        isErrorStatus = true
                    }
                }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Actual: $currentEmail", color = EcoColors.TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = "Nuevo Email",
                    icon = Icons.Default.Email
                )
            }
        }
    }

    // ── Cambio de Contraseña ──────────────────────────────────────────────
    if (showChangePasswordDialog) {
        ModernDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = "🔑 Seguridad",
            confirmButtonText = "Actualizar",
            onConfirm = {
                if (newPassword != repeatNewPassword) {
                    statusMessage = "❌ Las contraseñas no coinciden"
                    return@ModernDialog
                }
                scope.launch {
                    val success = UserRepository().updateUserPassword(newPassword)
                    if (success) {
                        statusMessage = "✅ Contraseña actualizada correctamente"
                        isErrorStatus = false
                        showChangePasswordDialog = false
                    } else {
                        statusMessage = "❌ Error: La contraseña debe tener min. 6 caracteres"
                        isErrorStatus = true
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = "Contraseña actual",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )
                ModernTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Nueva contraseña",
                    icon = Icons.Default.VpnKey,
                    isPassword = true
                )
                ModernTextField(
                    value = repeatNewPassword,
                    onValueChange = { repeatNewPassword = it },
                    label = "Repetir nueva contraseña",
                    icon = Icons.Default.VpnKey,
                    isPassword = true
                )
            }
        }
    }

    // ── Editar Perfil ─────────────────────────────────────────────────────
    if (showEditProfileDialog) {
        ModernDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = "👤 Editar Perfil",
            confirmButtonText = "Guardar",
            onConfirm = {
                scope.launch {
                    val success = UserRepository().updateProfileInfo(userId, newDisplayName, newBio)
                    if (success) {
                        settings.putString("username", newDisplayName)
                        // Sincronizar con cache de HistoryManager para consistencia entre pantallas
                        HistoryManager.updateProfileOffline(userId, "username", newDisplayName)
                        HistoryManager.updateProfileOffline(userId, "bio", newBio)
                        
                        currentDisplayName = newDisplayName
                        currentBio = newBio
                        statusMessage = "✅ Perfil actualizado correctamente"
                        isErrorStatus = false
                        showEditProfileDialog = false
                    } else {
                        statusMessage = "❌ Error al conectar con el servidor"
                        isErrorStatus = true
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = "Nombre de usuario",
                    icon = Icons.Default.Person
                )
                ModernTextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = "Biografía",
                    icon = Icons.Default.Info,
                    isSingleLine = false
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Ajustes", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Atrás", tint = EcoColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            if (userId.isNotEmpty()) {
                                HistoryManager.loadFromDatabase(userId, force = true)
                                PointsManager.forceSync()
                                statusMessage = "✅ Datos sincronizados de la nube"
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Sincronizar", tint = EcoColors.GlassAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EcoColors.BackgroundDark,
                    titleContentColor = EcoColors.TextPrimary,
                    navigationIconContentColor = EcoColors.TextPrimary
                )
            )
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Estado / feedback
            if (statusMessage.isNotEmpty()) {
                Surface(
                    color = if (isErrorStatus) EcoColors.Error.copy(alpha = 0.15f) else EcoColors.GlassGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isErrorStatus) EcoColors.Error.copy(alpha = 0.3f) else EcoColors.Success.copy(alpha = 0.3f))
                ) {
                    Text(
                        statusMessage, 
                        color = if (isErrorStatus) EcoColors.Error else EcoColors.Success, 
                        fontSize = 13.sp, 
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Sección: Cuenta ─────────────────────────────────────────────
            SectionHeader("👤 Cuenta")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // Info estática
                SettingsRow(Icons.Default.AccountCircle, Color(0xFF007AFF), "Usuario", currentDisplayName)
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(Icons.Default.Email, Color(0xFFFF9500), "Email", currentEmail)
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Acciones interactivas
                InteractiveRow(Icons.Default.Edit, Color(0xFF8E8E93), "Editar Perfil") {
                    newDisplayName = currentDisplayName
                    newBio = currentBio
                    showEditProfileDialog = true
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                InteractiveRow(Icons.Default.Email, Color(0xFFFF9500), "Cambiar Email") {
                    newEmail = ""
                    showChangeEmailDialog = true
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                InteractiveRow(Icons.Default.Lock, Color(0xFFFF3B30), "Cambiar Contraseña") {
                    oldPassword = ""
                    newPassword = ""
                    repeatNewPassword = ""
                    showChangePasswordDialog = true
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassSecondaryButton(
                    onClick = {
                        scope.launch {
                            UserRepository().signOut()
                            
                            // Limpieza profunda de datos locales
                            PointsManager.reset()
                            HistoryManager.clearHistory()
                            
                            // Limpiar otras preferencias de usuario
                            settings.remove("username")
                            settings.remove("email")
                            settings.remove("profile_pic_url")
                            settings.remove("collection_unlocked")
                            settings.remove("sync_done")
                            
                            onLogoutClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Sesión")
                }

                TextButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Eliminar Cuenta", color = EcoColors.Error, fontSize = 13.sp)
                }
            }

            // ── Sección: Apariencia ─────────────────────────────────────────
            SectionHeader("🎨 Apariencia")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(
                    icon = if (isDarkMode) CustomIcons.SunMoon else Icons.Default.LightMode,
                    iconColor = Color(0xFFFFCC00),
                    label = if (isDarkMode) "Modo Oscuro" else "Modo Claro",
                    checked = isDarkMode,
                    onCheckedChange = {
                        isDarkMode = it
                        settings.putBoolean("dark_mode", it)
                        EcoColors.updateTheme(it)
                    }
                )
            }

            SectionHeader("🔔 Notificaciones")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(CustomIcons.Bell, Color(0xFFFF3B30), "Notificaciones Push", pushEnabled) {
                    pushEnabled = it
                    settings.putBoolean("notify_push", it)
                    scope.launch {
                        UserRepository().updateNotificationPreferences(userId, it, rewardAlerts, emailAlerts)
                        statusMessage = "✅ Preferencias de avisos actualizadas"
                        
                        if (it) {
                            // Enviar notificación de prueba para confirmar que funciona
                            com.rubensimon.ecolens.utils.NotificationHelper.showNotification(
                                title = "EcoLens", 
                                message = "Las notificaciones reales ya están activas 🔔"
                            )
                        }
                    }
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(CustomIcons.Trophy, Color(0xFFFFCC00), "Nuevos Premios", rewardAlerts) {
                    rewardAlerts = it
                    settings.putBoolean("notify_rewards", it)
                    scope.launch {
                        UserRepository().updateNotificationPreferences(userId, pushEnabled, rewardAlerts, emailAlerts)
                        statusMessage = "✅ Ajustes de premios guardados"
                    }
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(Icons.Default.Mail, Color(0xFF007AFF), "Avisos por Email", emailAlerts) {
                    emailAlerts = it
                    settings.putBoolean("notify_email", it)
                    scope.launch {
                        UserRepository().updateNotificationPreferences(userId, pushEnabled, rewardAlerts, emailAlerts)
                        statusMessage = "✅ Preferencia de email actualizada"
                    }
                }
            }

            // ── Sección: Audio ──────────────────────────────────────────────
            SectionHeader("🎵 Audio")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(CustomIcons.Volume, Color(0xFFAF52DE), "Música de fondo", musicEnabled) {
                    musicEnabled = it
                    settings.putBoolean("audio_music_enabled", it)
                    if (it) com.rubensimon.ecolens.utils.PlatformAudio.playMusic("eco_ambient") 
                    else com.rubensimon.ecolens.utils.PlatformAudio.stopMusic()
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(CustomIcons.Volume, Color(0xFF5856D6), "Efectos de sonido", effectsEnabled) {
                    effectsEnabled = it
                    settings.putBoolean("audio_effects_enabled", it)
                }
            }

            // ── Sección: Privacidad y Legal ─────────────────────────────────
            SectionHeader("⚖️ Privacidad y Legal")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                InteractiveRow(Icons.Default.LocationOn, Color(0xFF34C759), "Permisos de Ubicación") { 
                    infoTitle = "📍 Ubicación"
                    infoText = "Los permisos de ubicación se solicitan automáticamente al abrir el Mapa. Si los has denegado, ve a los Ajustes de tu dispositivo para activarlos."
                    showInfoDialog = true
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                InteractiveRow(Icons.Default.Gavel, Color(0xFF8E8E93), "Términos y Condiciones") { 
                    infoTitle = "⚖️ Términos"
                    infoText = "Al usar EcoLens, te comprometes a reciclar de forma honesta. Los puntos no tienen valor monetario real."
                    showInfoDialog = true
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                InteractiveRow(CustomIcons.EyeOff, Color(0xFF555555), "Política de Privacidad") { 
                    infoTitle = "🔒 Privacidad"
                    infoText = "Tus datos de escaneo se usan solo para estadísticas de impacto ambiental. No compartimos tu ubicación con terceros."
                    showInfoDialog = true
                }
                EcoDivider(modifier = Modifier.padding(vertical = 8.dp))
                InteractiveRow(Icons.Default.Help, Color(0xFF007AFF), "Soporte / FAQ") { 
                    infoTitle = "💬 Soporte"
                    infoText = "¿Tienes problemas? Escríbenos a soporte@ecolens.com o visita nuestra web para tutoriales."
                    showInfoDialog = true
                }
            }

            // ── Sección: Datos ──────────────────────────────────────────────
            SectionHeader("🗑️ Datos locales")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                InteractiveRow(CustomIcons.Trash, Color(0xFFFF3B30), "Limpiar caché local") {
                    showClearDataDialog = true
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EcoLens v1.0.4", color = EcoColors.TextSecondary, fontSize = 12.sp)
                Text("Cuidando el planeta juntos", color = EcoColors.TextSecondary.copy(alpha = 0.6f), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Subcomposables ─────────────────────────────────────────────────────────

@Composable
private fun ModernDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButtonText: String,
    confirmColor: Color = EcoColors.GlassAccent,
    dismissButtonText: String = "Cancelar",
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = EcoColors.CardBackground.copy(alpha = 0.98f),
            cornerRadius = 32
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                content()
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp),
                        onClick = onDismissRequest,
                        color = EcoColors.CardPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.TextSecondary.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(dismissButtonText, color = EcoColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp),
                        onClick = onConfirm,
                        color = confirmColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(confirmButtonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isSingleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp), tint = EcoColors.GlassAccent) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = isSingleLine,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = EcoColors.CardPrimary,
            unfocusedContainerColor = EcoColors.CardPrimary,
            focusedTextColor = EcoColors.TextPrimary,
            unfocusedTextColor = EcoColors.TextPrimary,
            cursorColor = EcoColors.GlassAccent,
            focusedIndicatorColor = EcoColors.GlassAccent,
            unfocusedIndicatorColor = EcoColors.TextSecondary.copy(alpha = 0.3f),
            focusedLabelColor = EcoColors.GlassAccent,
            unfocusedLabelColor = EcoColors.TextSecondary
        )
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = EcoColors.GlassAccent,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsIconBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color
) {
    Surface(
        modifier = Modifier.size(28.dp),
        shape = RoundedCornerShape(7.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        SettingsIconBox(icon, iconColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = EcoColors.TextSecondary, fontSize = 11.sp)
            Text(value, color = EcoColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InteractiveRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            SettingsIconBox(icon, iconColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = EcoColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = EcoColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBox(icon, iconColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = EcoColors.TextPrimary, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759), // iOS Green
                uncheckedTrackColor = EcoColors.CardPrimary.copy(alpha = 0.5f)
            )
        )
    }
}
