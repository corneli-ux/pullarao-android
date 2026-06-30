package com.glm.aiapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.domain.model.Message
import com.glm.aiapp.domain.model.Role
import com.glm.aiapp.ui.components.EmptyState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: ChatViewModel = hiltViewModel()) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val activeConversation by vm.activeConversation.collectAsStateWithLifecycle()
    val streamingText by vm.streamingText.collectAsStateWithLifecycle()
    val streamingThinking by vm.streamingThinking.collectAsStateWithLifecycle()
    val isStreaming by vm.isStreaming.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll on new messages or streaming updates
    LaunchedEffect(activeConversation?.messages?.size, streamingText) {
        val total = (activeConversation?.messages?.size ?: 0) + if (streamingText.isNotBlank()) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // Sign-in nudge — only show when settings have loaded AND token is blank.
        // Don't show when settings is null (still loading from DataStore).
        val sessionToken = settings?.sessionToken?.trim().orEmpty()
        if (settings != null && sessionToken.isBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("⚠️ Sign in required", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Open Settings → Account → Sign in with your email and password. New here? Create an account at pullarao-appforge.vercel.app/register",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (activeConversation == null && conversations.isEmpty()) {
            EmptyState(
                title = "Start a conversation",
                subtitle = "Ask anything — Pullarao 1 streams responses in real time. Tap + to begin.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                activeConversation?.messages?.let { msgs ->
                    items(msgs, key = { it.id }) { msg -> MessageBubble(msg) }
                }
                if (streamingText.isNotBlank() || streamingThinking.isNotBlank()) {
                    item {
                        StreamingBubble(
                            text = streamingText,
                            thinking = streamingThinking,
                            model = settings?.chatParams?.model?.label ?: "GLM"
                        )
                    }
                }
                if (isStreaming && streamingText.isBlank() && streamingThinking.isBlank()) {
                    item { TypingIndicator() }
                }
            }
        }

        error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        // Composer
        Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.newConversation() }) {
                    Icon(Icons.Filled.Add, contentDescription = "New chat")
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message GLM…") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
                    leadingIcon = { Icon(Icons.Filled.SmartToy, contentDescription = null) }
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotBlank() && !isStreaming) {
                            vm.sendMessage(text)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !isStreaming,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, if (isUser) 4.dp else 16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                message.thinking?.takeIf { it.isNotBlank() }?.let {
                    Text("💭 Thinking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String, thinking: String, model: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (thinking.isNotBlank()) {
                    Text("💭 Thinking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(thinking, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
                if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Text(model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
