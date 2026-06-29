package com.glm.aiapp.ui.screens.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Google Sign-In result: code=${result.resultCode}, data=${result.data != null}")

        // Handle ALL result codes — not just RESULT_OK
        if (result.data == null) {
            vm.setError("Google Sign-In returned no data. Try email login instead.")
            return@rememberLauncherForActivityResult
        }

        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            Log.d("LoginScreen", "Got account: ${account?.email}, idToken=${account?.idToken != null}")

            val idToken = account?.idToken
            if (idToken != null) {
                vm.loginWithGoogle(idToken)
            } else {
                vm.setError("Google returned no ID token. This means the Web Client ID (714767483567.apps.googleusercontent.com) is not registered as a Web Application OAuth client in Google Cloud Console. Go to console.cloud.google.com → APIs & Services → Credentials → check the Web Application client ID, then update LoginScreen.kt line 68.")
            }
        } catch (e: ApiException) {
            Log.e("LoginScreen", "Google Sign-In ApiException", e)
            val msg = when (e.statusCode) {
                10 -> "DEVELOPER_ERROR (code 10). The Web Client ID or SHA-1 is wrong. Go to console.cloud.google.com → APIs & Services → Credentials and check: 1) there's a Web Application OAuth client with ID 714767483567.apps.googleusercontent.com, 2) the Android OAuth client has package com.glm.aiapp and SHA-1 88:3F:61:28:7B:28:90:28:67:65:A9:61:9E:A3:C1:5B:25:B0:84:2D"
                12500 -> "Google Play services error (code 12500). Update Google Play Services on your device."
                12501 -> "Sign-in cancelled."
                7 -> "Network error. Check your internet connection."
                else -> "Google Sign-In failed (code ${e.statusCode}): ${e.message}"
            }
            vm.setError(msg)
        } catch (e: Exception) {
            Log.e("LoginScreen", "Google Sign-In unexpected error", e)
            vm.setError("Unexpected error: ${e.message}")
        }
    }

    fun launchGoogleSignIn() {
        val webClientId = "714767483567-jgo1ls597k3gqs7pcu97j941rndrs9bh.apps.googleusercontent.com"
        Log.d("LoginScreen", "Launching Google Sign-In with webClientId=$webClientId")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // Sign out first so account picker shows
        client.signOut()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginScreen", "Sign-out successful, launching sign-in intent")
                    try {
                        googleSignInLauncher.launch(client.signInIntent)
                    } catch (e: Exception) {
                        Log.e("LoginScreen", "Failed to launch sign-in intent", e)
                        vm.setError("Failed to start Google Sign-In: ${e.message}")
                    }
                } else {
                    Log.e("LoginScreen", "Sign-out failed", task.exception)
                    // Try launching anyway — sign-out failure shouldn't block sign-in
                    try {
                        googleSignInLauncher.launch(client.signInIntent)
                    } catch (e: Exception) {
                        vm.setError("Failed to start Google Sign-In: ${e.message}")
                    }
                }
            }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F766E), Color(0xFF134E4A)),
                    start = Offset(0f, gradientOffset * 300),
                    end = Offset(1000f, 1500f + gradientOffset * 300)
                )
            )
    ) {
        Orb(modifier = Modifier.offset(x = (-50).dp, y = 100.dp), color = Color(0xFF10B981).copy(alpha = 0.3f), size = 200.dp)
        Orb(modifier = Modifier.offset(x = 250.dp, y = 400.dp), color = Color(0xFFF59E0B).copy(alpha = 0.2f), size = 250.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

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
            Spacer(Modifier.height(40.dp))

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
                        Surface(
                            onClick = { launchGoogleSignIn() },
                            enabled = !state.isSubmitting,
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("G", color = Color(0xFF4285F4), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                                Text("Continue with Google", color = Color(0xFF1F2937), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                            Text("or", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            onClick = { showEmailForm = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text("Continue with email", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { vm.setEmail(it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            colors = fieldColors()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { vm.setPassword(it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = fieldColors()
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { vm.loginWithEmail() },
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

            // Error display — always visible when there's an error
            state.error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚠️ Error",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            msg,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Loading indicator
            if (state.isSubmitting) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF10B981))
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "By continuing you agree to our Terms\nand acknowledge our Privacy Policy",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF10B981),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color(0xFF10B981),
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
)

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
