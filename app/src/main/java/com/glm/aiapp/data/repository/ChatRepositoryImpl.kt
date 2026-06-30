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
import java.util.UUID
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
        if (settings.sessionToken.isBlank()) {
            error("Not signed in. Open Settings → Account → Sign in with your email and password.")
        }
        val conv = db.conversationDao().getById(conversationId)
            ?: error("Conversation $conversationId not found")
        val history = db.messageDao().observeByConversation(conversationId).first()

        // Build the messages array using org.json (simpler than kotlinx.serialization for this case)
        val messagesJson = org.json.JSONArray().apply {
            if (conv.systemPrompt.isNotBlank()) {
                put(org.json.JSONObject().put("role", "system").put("content", conv.systemPrompt))
            }
            history.forEach { m ->
                put(org.json.JSONObject().put("role", m.role.lowercase()).put("content", m.content))
            }
        }
        val requestBody = org.json.JSONObject()
            .put("messages", messagesJson)
            .put("temperature", Math.round(params.temperature * 100.0) / 100.0)
            .put("max_tokens", params.maxTokens)
            .toString()

        val platformUrl = settings.platformUrl.trimEnd('/')
        val url = "$platformUrl/api/glm/chat"

        val contentBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        // The platform returns SSE events: { "type": "token"|"done"|"error", "content"|"message" }
        streamClient.streamAuthorized(url, settings.sessionToken, requestBody).collect { evt ->
            val type = evt.optString("type", "")
            when (type) {
                "token" -> {
                    val tok = evt.optString("content", "")
                    if (tok.isNotEmpty()) { onToken(tok); contentBuilder.append(tok) }
                }
                "done" -> { /* stream finished */ }
                "error" -> error(evt.optString("message", "Chat request failed"))
            }
        }

        val msg = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = Role.ASSISTANT,
            content = contentBuilder.toString(),
            thinking = thinkingBuilder.toString().ifBlank { null },
            tokens = null,
            createdAt = System.currentTimeMillis()
        )
        appendMessage(conversationId, msg)
        return msg
    }

    // ---- Mappers ----

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        systemPrompt = systemPrompt,
        model = model,
        thinkingEnabled = thinkingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        conversationId = conversationId,
        role = Role.valueOf(role.uppercase()),
        content = content,
        thinking = thinking,
        tokens = tokens,
        createdAt = createdAt
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name.lowercase(),
        content = content,
        thinking = thinking,
        tokens = tokens,
        createdAt = createdAt
    )
}
