package com.rubensimon.ecolens.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.PointsManager
import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ecolens.composeapp.generated.resources.Res
import ecolens.composeapp.generated.resources.logo_ecolens
import androidx.compose.foundation.Image

/**
 * Pantalla de Login/Registro de EcoLens — migrada de LoginActivity.
 *
 * Autenticación con Supabase Auth (Email/Password).
 * Tras login exitoso llama a [onLoginSuccess].
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings = remember { Settings() }
    val client = remember { SupabaseClientProvider.client }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Very light gray background
    ) {
        // Green Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF059669)
                        )
                    )
                )
        ) {
            // Pills decorative
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 200.dp)
                    .offset(x = 20.dp, y = (-20).dp)
                    .rotate(45f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🌱", fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "EcoLens",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // White Card
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 320.dp),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignUpMode) "hello!" else "welcome back!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                if (isSignUpMode) {
                    ModernTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        icon = Icons.Default.Person,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                ModernTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    icon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = CustomIcons.EyeOff,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill all fields"
                            return@Button
                        }
                        errorMessage = ""
                        isLoading = true
                        scope.launch {
                            try {
                                if (isSignUpMode) {
                                    client.auth.signUpWith(Email) { this.email = email.trim(); this.password = password }
                                    if (username.isNotBlank()) {
                                        UserRepository().createOrUpdateUser(username = username.trim(), puntos = 0, totalScans = 0)
                                    }
                                } else {
                                    client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
                                }
                                val user = client.auth.currentSessionOrNull()?.user
                                val userId = user?.id ?: ""
                                val userEmail = user?.email ?: email
                                settings.putString("user_id", userId)
                                settings.putString("email", userEmail)
                                settings.putString("username", if (isSignUpMode) username else userEmail.substringBefore("@"))
                                PointsManager.setUserId(userId)
                                PointsManager.loadFromSupabase(userId)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Error"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            if (isSignUpMode) "Sign Up" else "Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { isSignUpMode = !isSignUpMode; errorMessage = "" }) {
                        Text(
                            if (isSignUpMode) "Login" else "Register",
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(),
        singleLine = true
    )
}
