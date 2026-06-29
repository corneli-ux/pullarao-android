package com.glm.aiapp.ui.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(vm: LoginViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEmailForm by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    vm.loginWithGoogle(idToken)
                }
            } catch (_: Exception) {
                // User cancelled or error
            }
        }
    }

    fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1035738900117-7n0vqjq8e9r8m4f2k2k2k2k2k2k2k2k2.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F766E),
                        Color(0xFF134E4A)
                    ),
                    start = Offset(0f, gradientOffset * 300),
                    end = Offset(1000f, 1500f + gradientOffset * 300)
                )
            )
    ) {
        // Floating gradient orbs
        Orb(modifier = Modifier.offset(x = (-50).dp, y = 100.dp), color = Color(0xFF10B981).copy(alpha = 0.3f), size = 200.dp)
        Orb(modifier = Modifier.offset(x = 250.dp, y = 400.dp), color = Color(0xFFF59E0B).copy(alpha = 0.2f), size = 250.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF10B981), Color(0xFF0F766E))))
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("P1", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            Text("Pullarao 1", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Build apps with AI", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)

            Spacer(Modifier.height(48.dp))

            // Animated content
            AnimatedContent(
                targetState = showEmailForm,
                transitionSpec = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) togetherWith
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                },
                label = "form"
            ) { showEmail ->
                if (!showEmail) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Google button
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F2937)
                            )
                        ) {
                            Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Divider
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                            Text("or", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Email button
                        OutlinedButton(
                            onClick = { showEmailForm = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Continue with email", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    // Email/password form
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = vm::setEmail,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF10B981)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = vm::setPassword,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF10B981)
                            )
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = vm::loginWithEmail,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            enabled = !state.isSubmitting && state.email.isNotBlank() && state.password.isNotBlank()
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Sign in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { showEmailForm = false }) {
                            Text("← Back", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(visible = state.error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                state.error?.let { msg ->
                    Spacer(Modifier.height(16.dp))
                    Surface(color = Color(0xFFEF4444).copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(msg, color = Color(0xFFFCA5A5), fontSize = 13.sp, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                    }
                }
            }

            // Loading overlay
            if (state.isSubmitting) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF10B981))
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "By continuing you agree to our Terms\nand acknowledge our Privacy Policy",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Orb(modifier: Modifier, color: Color, size: Dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = listOf(color, Color.Transparent)))
            .blur(40.dp)
    )
}
