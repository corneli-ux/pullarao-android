package com.glm.aiapp.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glm.aiapp.domain.model.AppSettings
import com.glm.aiapp.domain.model.ChatParams
import com.glm.aiapp.domain.model.ThemeMode
import com.glm.aiapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun updatePlatformUrl(url: String) = viewModelScope.launch { repo.updatePlatformUrl(url) }
    fun setSession(token: String, email: String, name: String) = viewModelScope.launch { repo.setSession(token, email, name) }
    fun clearSession() = viewModelScope.launch { repo.clearSession() }
    fun updateChatParams(params: ChatParams) = viewModelScope.launch { repo.updateChatParams(params) }
    fun updateTheme(mode: ThemeMode) = viewModelScope.launch { repo.updateThemeMode(mode) }
}
