package com.glm.aiapp.data.repository

import com.glm.aiapp.data.prefs.SettingsStore
import com.glm.aiapp.domain.model.ChatParams
import com.glm.aiapp.domain.model.ThemeMode
import com.glm.aiapp.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsStore
) : SettingsRepository {

    override val settings = store.settings

    override suspend fun updatePlatformUrl(url: String) { store.updatePlatformUrl(url) }
    override suspend fun setSession(token: String, email: String, name: String) { store.setSession(token, email, name) }
    override suspend fun clearSession() { store.clearSession() }
    override suspend fun updateChatParams(params: ChatParams) { store.updateChatParams(params) }
    override suspend fun updateThemeMode(mode: ThemeMode) { store.updateThemeMode(mode) }
}
