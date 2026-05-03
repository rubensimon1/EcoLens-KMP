package com.rubensimon.ecolens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.Settings

/**
 * Única Activity del proyecto KMP — punto de entrada Android.
 *
 * Reemplaza TODAS las Activities del Android original:
 * LoginActivity, MenuActivity, MainActivity, ProfileActivity,
 * SettingsActivity, HistoryActivity, LeaderboardActivity,
 * RewardsActivity, MapsActivity, CollectionActivity, UpcyclingActivity.
 *
 * La navegación se gestiona con NavController en [App] → [AppNavigation].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inicializar utilidades nativas
        com.rubensimon.ecolens.utils.PlatformAudio.setContext(this)
        com.rubensimon.ecolens.utils.NotificationHelper.setContext(this)
        com.rubensimon.ecolens.utils.NotificationHelper.createNotificationChannel(this)

        setContent {
            // Comprobar si hay sesión guardada para saltar al menú
            val settings = remember { Settings() }
            val savedUserId = remember { settings.getString("user_id", "") }
            val initialRoute = if (savedUserId.isNotEmpty()) "menu" else "welcome"

            App(startDestination = initialRoute)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
