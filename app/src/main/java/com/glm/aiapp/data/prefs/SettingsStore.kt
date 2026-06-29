package com.glm.aiapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.glm.aiapp.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pullarao_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val PLATFORM_URL = stringPreferencesKey("platform_url")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TOP_P = floatPreferencesKey("top_p")
        val THINKING = stringPreferencesKey("thinking")
        val STREAMING = booleanPreferencesKey("streaming")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val THEME = stringPreferencesKey("theme")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            platformUrl = p[Keys.PLATFORM_URL] ?: DEFAULT_PLATFORM_URL,
            sessionToken = p[Keys.SESSION_TOKEN] ?: "",
            userEmail = p[Keys.USER_EMAIL] ?: "",
            userName = p[Keys.USER_NAME] ?: "",
            chatParams = ChatParams(
                model = ChatModel.fromId(p[Keys.MODEL]),
                temperature = p[Keys.TEMPERATURE] ?: 0.7f,
                maxTokens = p[Keys.MAX_TOKENS] ?: 4096,
                topP = p[Keys.TOP_P] ?: 0.9f,
                thinking = ThinkingMode.entries.firstOrNull { it.id == p[Keys.THINKING] } ?: ThinkingMode.DISABLED,
                systemPrompt = p[Keys.SYSTEM_PROMPT] ?: "",
                streaming = p[Keys.STREAMING] ?: true
            ),
            themeMode = ThemeMode.entries.firstOrNull { it.name.equals(p[Keys.THEME], true) } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun updatePlatformUrl(url: String) = context.dataStore.edit { it[Keys.PLATFORM_URL] = url }
    suspend fun setSession(token: String, email: String, name: String) = context.dataStore.edit {
        it[Keys.SESSION_TOKEN] = token
        it[Keys.USER_EMAIL] = email
        it[Keys.USER_NAME] = name
    }
    suspend fun clearSession() = context.dataStore.edit {
        it.remove(Keys.SESSION_TOKEN)
        it.remove(Keys.USER_EMAIL)
        it.remove(Keys.USER_NAME)
    }
    suspend fun updateChatParams(params: ChatParams) = context.dataStore.edit {
        it[Keys.MODEL] = params.model.id
        it[Keys.TEMPERATURE] = params.temperature
        it[Keys.MAX_TOKENS] = params.maxTokens
        it[Keys.TOP_P] = params.topP
        it[Keys.THINKING] = params.thinking.id
        it[Keys.STREAMING] = params.streaming
        it[Keys.SYSTEM_PROMPT] = params.systemPrompt
    }
    suspend fun updateThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
}
