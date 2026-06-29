package com.glm.aiapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glm.aiapp.domain.model.ChatParams
import com.glm.aiapp.domain.model.Conversation
import com.glm.aiapp.domain.model.Message
import com.glm.aiapp.domain.model.Role
import com.glm.aiapp.domain.repository.ChatRepository
import com.glm.aiapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> =
        chatRepo.observeConversations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId

    val activeConversation: StateFlow<Conversation?> =
        _activeConversationId
            .filterNotNull()
            .flatMapLatest { id -> chatRepo.observeConversation(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText

    private val _streamingThinking = MutableStateFlow("")
    val streamingThinking: StateFlow<String> = _streamingThinking

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun selectConversation(id: String) { _activeConversationId.value = id }

    fun newConversation() {
        viewModelScope.launch {
            val s = settings.value
            val conv = chatRepo.createConversation(
                title = "New chat",
                systemPrompt = s?.chatParams?.systemPrompt ?: "",
                model = s?.chatParams?.model?.id ?: "glm-4.6"
            )
            _activeConversationId.value = conv.id
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val conversationId = _activeConversationId.value ?: run {
            viewModelScope.launch {
                val s = settings.value
                val conv = chatRepo.createConversation(
                    title = text.take(40),
                    systemPrompt = s?.chatParams?.systemPrompt ?: "",
                    model = s?.chatParams?.model?.id ?: "glm-4.6"
                )
                _activeConversationId.value = conv.id
                sendAfterCreate(conv.id, text)
            }
            return
        }
        viewModelScope.launch { sendAfterCreate(conversationId, text) }
    }

    private suspend fun sendAfterCreate(conversationId: String, text: String) {
        val params = settings.value?.chatParams ?: ChatParams()
        val apiKey = settings.value?.apiKey?.trim().orEmpty()

        if (apiKey.isBlank()) {
            _error.value = "No API key configured. Open Settings → Connection → API key and paste your GLM API key (get one at https://open.bigmodel.cn/usercenter/apikeys)."
            return
        }

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = Role.USER,
            content = text,
            createdAt = System.currentTimeMillis()
        )
        chatRepo.appendMessage(conversationId, userMsg)

        _isStreaming.value = true
        _streamingText.value = ""
        _streamingThinking.value = ""
        _error.value = null

        try {
            chatRepo.streamChat(
                conversationId = conversationId,
                params = params,
                onToken = { tok -> _streamingText.value += tok },
                onThinking = { th -> _streamingThinking.value += th }
            )
        } catch (t: Throwable) {
            _error.value = t.message ?: "Chat request failed"
        } finally {
            _isStreaming.value = false
            _streamingText.value = ""
            _streamingThinking.value = ""
        }
    }

    fun clearError() { _error.value = null }
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepo.deleteConversation(id)
            if (_activeConversationId.value == id) _activeConversationId.value = null
        }
    }
}
