package com.glm.aiapp.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glm.aiapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val userName: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setEmail(v: String) { _state.value = _state.value.copy(email = v) }
    fun setPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun setError(msg: String) { _state.value = _state.value.copy(error = msg, isSubmitting = false) }

    private suspend fun getPlatformUrl(): String {
        return settingsRepo.settings.first().platformUrl.trimEnd('/')
    }

    fun loginWithEmail() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                val url = getPlatformUrl()
                val result = withContext(Dispatchers.IO) {
                    val payload = JSONObject()
                        .put("email", s.email.trim().lowercase())
                        .put("password", s.password)
                        .toString()
                    val req = Request.Builder()
                        .url("$url/api/mobile/login")
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string() ?: ""
                        if (!res.isSuccessful) {
                            val msg = JSONObject(body).optString("error", "Login failed (${res.code})")
                            throw RuntimeException(msg)
                        }
                        JSONObject(body)
                    }
                }
                saveSession(result)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSubmitting = false, error = e.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            try {
                val url = getPlatformUrl()
                val result = withContext(Dispatchers.IO) {
                    val payload = JSONObject().put("idToken", idToken).toString()
                    val req = Request.Builder()
                        .url("$url/api/mobile/login/google")
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string() ?: ""
                        if (!res.isSuccessful) {
                            val msg = if (body.isNotBlank()) {
                                try { JSONObject(body).optString("error", "Google login failed (${res.code})") }
                                catch (_: Exception) { "Google login failed (HTTP ${res.code}, body: ${body.take(100)})" }
                            } else {
                                "Google login failed (HTTP ${res.code}, empty response from server)"
                            }
                            throw RuntimeException(msg)
                        }
                        if (body.isBlank()) {
                            throw RuntimeException("Server returned empty response")
                        }
                        JSONObject(body)
                    }
                }
                saveSession(result)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSubmitting = false, error = e.message ?: "Google login failed")
            }
        }
    }

    private suspend fun saveSession(result: JSONObject) {
        val token = result.getString("token")
        val user = result.optJSONObject("user")
        val email = user?.optString("email") ?: ""
        val name = user?.optString("name") ?: ""
        settingsRepo.setSession(token, email, name)
        _state.value = LoginUiState(success = true, userName = name.ifBlank { email })
    }

    fun logout() {
        viewModelScope.launch { settingsRepo.clearSession() }
        _state.value = LoginUiState()
    }
}
