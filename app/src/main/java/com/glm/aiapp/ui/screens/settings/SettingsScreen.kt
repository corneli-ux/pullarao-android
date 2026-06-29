package com.glm.aiapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.domain.model.ChatModel
import com.glm.aiapp.domain.model.ThinkingMode
import com.glm.aiapp.domain.model.ThemeMode
import com.glm.aiapp.ui.theme.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    val s = settings ?: return
    var apiKey by remember(s.apiKey) { mutableStateOf(s.apiKey) }
    var baseUrl by remember(s.baseUrl) { mutableStateOf(s.baseUrl) }
    var systemPrompt by remember(s.chatParams.systemPrompt) { mutableStateOf(s.chatParams.systemPrompt) }
    var temperature by remember(s.chatParams.temperature) { mutableStateOf(s.chatParams.temperature) }
    var maxTokens by remember(s.chatParams.maxTokens) { mutableStateOf(s.chatParams.maxTokens) }
    var topP by remember(s.chatParams.topP) { mutableStateOf(s.chatParams.topP) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection
        SectionCard(title = "Connection") {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; vm.updateApiKey(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") }
                }
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; vm.updateBaseUrl(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true
            )
        }

        // Model params
        SectionCard(title = "Model") {
            Text("Default model", style = MaterialTheme.typography.labelLarge)
            ChatModel.entries.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = s.chatParams.model == m,
                        onClick = { vm.updateChatParams(s.chatParams.copy(model = m)) }
                    )
                    Column {
                        Text(m.label, style = MaterialTheme.typography.bodyMedium)
                        Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Thinking mode", style = MaterialTheme.typography.labelLarge)
            ThinkingMode.entries.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = s.chatParams.thinking == t,
                        onClick = { vm.updateChatParams(s.chatParams.copy(thinking = t)) }
                    )
                    Text(t.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it; vm.updateChatParams(s.chatParams.copy(systemPrompt = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("System prompt") },
                minLines = 2
            )
        }

        // Generation params
        SectionCard(title = "Generation") {
            Text("Temperature: ${"%.2f".format(temperature)}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = temperature,
                onValueChange = { temperature = it; vm.updateChatParams(s.chatParams.copy(temperature = it)) },
                valueRange = 0f..2f,
                steps = 19
            )
            Text("Max tokens: $maxTokens", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = { maxTokens = it.toInt(); vm.updateChatParams(s.chatParams.copy(maxTokens = it.toInt())) },
                valueRange = 256f..32768f,
                steps = 200
            )
            Text("Top P: ${"%.2f".format(topP)}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = topP,
                onValueChange = { topP = it; vm.updateChatParams(s.chatParams.copy(topP = it)) },
                valueRange = 0f..1f,
                steps = 19
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = s.chatParams.streaming,
                    onCheckedChange = { vm.updateChatParams(s.chatParams.copy(streaming = it)) }
                )
                Spacer(Modifier.width(8.dp))
                Text("Stream responses (SSE)", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Theme
        SectionCard(title = "Appearance") {
            Text("Theme", style = MaterialTheme.typography.labelLarge)
            ThemeMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = s.themeMode == mode,
                        onClick = { vm.updateTheme(mode) }
                    )
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // About
        SectionCard(title = "About") {
            Text("Pullarao 1 App", style = MaterialTheme.typography.titleSmall)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Built with Jetpack Compose · Hilt · Room · Retrofit · Pullarao 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
