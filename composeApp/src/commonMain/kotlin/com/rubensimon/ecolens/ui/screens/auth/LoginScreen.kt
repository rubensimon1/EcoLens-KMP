package com.rubensimon.ecolens.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import com.rubensimon.ecolens.utils.PointsManager
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email as SupabaseEmail
import kotlinx.coroutines.launch

enum class AuthState { Welcome, Login }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    startInWelcome: Boolean = false,
    modifier: Modifier = Modifier
) {
    val client = SupabaseClientProvider.client
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val backgroundOffset by animateFloatAsState(
        targetValue = if (isForgotPasswordMode) 80f else if (isSignUpMode) 40f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f)
    )
    var authState by remember { mutableStateOf(if (startInWelcome) AuthState.Welcome else AuthState.Login) }
    
    val loginMode = when {
        isForgotPasswordMode -> "forgot"
        isSignUpMode -> "signup"
        else -> "signin"
    }

    val scope = rememberCoroutineScope()
    val settings = Settings()

    val oledBlack = Color(0xFF000000)
    val vividTurquoise = Color(0xFF1DE9B6)
    val darkGreenBg = Color(0xFF134533)

    val whiteShapeStartY by animateFloatAsState(
        targetValue = if (authState == AuthState.Welcome) 0.65f else 0.18f,
        animationSpec = tween(durationMillis = 800)
    )
    val whiteShapeEndY by animateFloatAsState(
        targetValue = if (authState == AuthState.Welcome) 0.90f else 0.65f,
        animationSpec = tween(durationMillis = 800)
    )
    // Detectar si el teclado (IME) está visible
    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    val formTopSpace by animateFloatAsState(
        targetValue = when {
            isKeyboardOpen -> 0.05f
            authState == AuthState.Welcome -> 0.28f
            else -> 0.32f
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(darkGreenBg)
    ) {
        val screenHeight = maxHeight
        
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..7) {
                    val progress = i / 7f
                    val startY = size.height * (1.1f - progress * 0.9f) + backgroundOffset
                    val endY = size.height * (1.0f - progress * 0.8f) + backgroundOffset
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, startY)
                        cubicTo(size.width * 0.3f, startY + size.height * 0.1f, size.width * 0.7f, endY - size.height * 0.1f, size.width, endY)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, color = darkGreenBg)
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height * whiteShapeStartY)
                    cubicTo(size.width * 0.5f, size.height * whiteShapeStartY, size.width * 0.4f, size.height * whiteShapeEndY, size.width, size.height * whiteShapeEndY)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = Color.White)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(horizontal = 40.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(screenHeight * formTopSpace))

                AnimatedContent(
                    targetState = authState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.92f, animationSpec = tween(600))).togetherWith(
                         fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.08f, animationSpec = tween(600)))
                    }
                ) { state ->
                    when (state) {
                        AuthState.Welcome -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Welcome", fontSize = 46.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "Escanea. Recicla. Salva el planeta.", color = Color.White.copy(alpha = 0.8f), fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 32.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Continue", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
                                    IconButton(
                                        onClick = { if (settings.getBoolean("onboarding_completed", false)) authState = AuthState.Login else onNavigateToOnboarding() },
                                        modifier = Modifier.size(56.dp).shadow(8.dp, CircleShape, spotColor = vividTurquoise).background(vividTurquoise, CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = "Continue", tint = Color.White)
                                    }
                                }
                            }
                        }
                        AuthState.Login -> {
                            AnimatedContent(
                                targetState = loginMode,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it / 4 }).togetherWith(
                                     fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500)) { -it / 4 })
                                }
                            ) { mode ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(text = if (mode == "forgot") "Reset Password" else if (mode == "signup") "Sign up" else "Sign in", fontSize = 44.sp, fontWeight = FontWeight.Black, color = oledBlack)
                                    Spacer(modifier = Modifier.height(32.dp))

                                    if (mode == "forgot") {
                                        ImageTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = "Old Password", icon = Icons.Outlined.Lock, isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                        ImageTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New Password", icon = Icons.Outlined.Lock, isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                        ImageTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm New Password", icon = Icons.Outlined.Lock, isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                    } else {
                                        if (mode == "signup") {
                                            ImageTextField(value = username, onValueChange = { username = it }, label = "Username", icon = Icons.Outlined.Person)
                                        }
                                        ImageTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Outlined.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                                        ImageTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Outlined.Lock, isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                    }

                                    if (mode == "signin") {
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Forgot Password?", fontSize = 12.sp, color = vividTurquoise, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isForgotPasswordMode = true; errorMessage = "" })
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(32.dp))
                                    }

                                    if (errorMessage.isNotEmpty()) {
                                        Text(text = errorMessage, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (mode == "forgot") {
                                                if (newPassword != confirmPassword) { errorMessage = "Passwords don't match"; return@Button }
                                                isForgotPasswordMode = false
                                            } else {
                                                if (email.isBlank() || password.isBlank()) { errorMessage = "Fill all fields"; return@Button }
                                                isLoading = true
                                                scope.launch {
                                                    try {
                                                        if (mode == "signup") {
                                                            client.auth.signUpWith(SupabaseEmail) { this.email = email.trim(); this.password = password }
                                                            if (username.isNotBlank()) UserRepository().createOrUpdateUser(username = username.trim(), puntos = 0, totalScans = 0)
                                                        } else {
                                                            client.auth.signInWith(SupabaseEmail) { this.email = email.trim(); this.password = password }
                                                        }
                                                        val user = client.auth.currentSessionOrNull()?.user
                                                        settings.putString("user_id", user?.id ?: "")
                                                        onLoginSuccess()
                                                    } catch (e: Exception) { errorMessage = e.message ?: "Error" } finally { isLoading = false }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = vividTurquoise),
                                        colors = ButtonDefaults.buttonColors(containerColor = vividTurquoise),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                        else Text(text = if (mode == "forgot") "Change Password" else if (mode == "signup") "Sign Up" else "Login", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.Center) {
                                        Text(if (mode == "signup") "Already have an Account? " else "Don't have an Account? ", color = Color.Gray, fontSize = 13.sp)
                                        Text(if (mode == "signup") "Sign in" else "Sign up", color = vividTurquoise, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { isSignUpMode = !isSignUpMode; isForgotPasswordMode = false; errorMessage = "" })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }
}

@Composable
fun ImageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(text = label.uppercase(), color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) Text(text = "enter your ${label.lowercase()}", color = Color.LightGray, fontSize = 14.sp)
                BasicTextField(
                    value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = keyboardOptions, singleLine = true
                )
            }
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
    }
}
