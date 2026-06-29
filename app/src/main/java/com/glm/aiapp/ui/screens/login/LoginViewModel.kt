package com.glm.aiapp.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glm.aiapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun login(platformUrl: String) {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    val payload = JSONObject()
                        .put("email", s.email.trim().lowercase())
                        .put("password", s.password)
                        .toString()
                    val req = Request.Builder()
                        .url("${platformUrl.trimEnd('/')}/api/mobile/login")
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
                val token = result.getString("token")
                val user = result.optJSONObject("user")
                val email = user?.optString("email") ?: s.email
                val name = user?.optString("name") ?: ""
                settingsRepo.setSession(token, email, name)
                _state.value = LoginUiState(success = true, userName = name.ifBlank { email })
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSubmitting = false, error = e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { settingsRepo.clearSession() }
        _state.value = LoginUiState()
    }
}
