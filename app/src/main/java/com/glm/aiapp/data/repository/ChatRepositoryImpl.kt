package com.glm.aiapp.data.repository

import com.glm.aiapp.data.api.GlmApi
import com.glm.aiapp.data.api.StreamingChatClient
import com.glm.aiapp.data.db.AppDatabase
import com.glm.aiapp.data.db.ConversationEntity
import com.glm.aiapp.data.db.MessageEntity
import com.glm.aiapp.data.dto.*
import com.glm.aiapp.domain.model.*
import com.glm.aiapp.domain.repository.ChatRepository
import com.glm.aiapp.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val api: GlmApi,
    private val streamClient: StreamingChatClient,
    private val settingsRepo: SettingsRepository,
    private val json: Json
) : ChatRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun observeConversations(): Flow<List<Conversation>> =
        db.conversationDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeConversation(id: String): Flow<Conversation?> =
        combine(
            db.conversationDao().observeById(id),
            db.messageDao().observeByConversation(id)
        ) { conv, msgs ->
            conv?.toDomain()?.copy(messages = msgs.map { it.toDomain() })
        }

    override suspend fun createConversation(title: String, systemPrompt: String, model: String): Conversation {
        val now = System.currentTimeMillis()
        val conv = ConversationEntity(UUID.randomUUID().toString(), title, systemPrompt, model, false, now, now)
        db.conversationDao().upsert(conv)
        return conv.toDomain()
    }

    override suspend fun deleteConversation(id: String) = db.conversationDao().delete(id)

    override suspend fun appendMessage(conversationId: String, message: Message) {
        db.messageDao().upsert(message.toEntity())
        db.conversationDao().touch(conversationId, System.currentTimeMillis())
    }

    override suspend fun updateMessage(message: Message) = db.messageDao().upsert(message.toEntity())

    override suspend fun streamChat(
        conversationId: String,
        params: ChatParams,
        onToken: (String) -> Unit,
        onThinking: (String) -> Unit
    ): Message {
        val settings = settingsRepo.settings.first()
        if (settings.sessionToken.isBlank()) error("Not signed in.")

        val conv = db.conversationDao().getById(conversationId)
            ?: error("Conversation not found")
        val history = db.messageDao().observeByConversation(conversationId).first()

        // Build messages — filter out empty content
        val messagesJson = JSONArray().apply {
            if (conv.systemPrompt.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", conv.systemPrompt))
            }
            history.forEach { m ->
                if (m.content.isNotBlank()) {
                    put(JSONObject().put("role", m.role.lowercase()).put("content", m.content))
                }
            }
        }

        val requestBody = JSONObject()
            .put("messages", messagesJson)
            .put("temperature", Math.round(params.temperature * 100.0) / 100.0)
            .put("max_tokens", 8192)
            .toString()

        val platformUrl = settings.platformUrl.trimEnd('/')
        val url = "$platformUrl/api/glm/chat-sync"

        // Make the HTTP call on IO dispatcher — the ENTIRE call including response parsing
        val content = withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${settings.sessionToken}")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                val body = res.body?.string() ?: ""
                if (!res.isSuccessful) {
                    val msg = if (body.isNotBlank()) {
                        try { JSONObject(body).optString("error", "HTTP ${res.code}") }
                        catch (_: Exception) { "Server error (HTTP ${res.code})" }
                    } else {
                        "Server error (HTTP ${res.code})"
                    }
                    error(msg)
                }
                if (body.isBlank()) error("Server returned empty response")
                val json = JSONObject(body)
                val content = json.optString("content", "")
                val reasoning = json.optString("reasoning", "")
                Pair(content, reasoning)
            }
        }

        if (content.isBlank()) error("Pullarao returned an empty response.")

        // Show reasoning first (if available), then the response
        if (reasoning.isNotBlank()) {
            onThinking(reasoning)
        }
        onToken(content)

        // Save assistant message to DB
        val msg = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = Role.ASSISTANT,
            content = content,
            thinking = reasoning.ifBlank { null },
            tokens = null,
            createdAt = System.currentTimeMillis()
        )
        appendMessage(conversationId, msg)
        return msg
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id, title = title, systemPrompt = systemPrompt, model = model,
        thinkingEnabled = thinkingEnabled, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id, conversationId = conversationId, role = Role.valueOf(role.uppercase()),
        content = content, thinking = thinking, tokens = tokens, createdAt = createdAt
    )

    private fun Message.toEntity() = MessageEntity(
        id = id, conversationId = conversationId, role = role.name.lowercase(),
        content = content, thinking = thinking, tokens = tokens, createdAt = createdAt
    )
}
