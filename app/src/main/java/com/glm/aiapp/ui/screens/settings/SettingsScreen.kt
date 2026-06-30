package com.glm.aiapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.domain.model.ChatModel
import com.glm.aiapp.domain.model.ThinkingMode
import com.glm.aiapp.domain.model.ThemeMode
import com.glm.aiapp.ui.theme.SettingsViewModel
import com.glm.aiapp.ui.screens.login.LoginViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel(), loginVm: LoginViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    val s = settings ?: return
    var systemPrompt by remember(s.chatParams.systemPrompt) { mutableStateOf(s.chatParams.systemPrompt) }
    var temperature by remember(s.chatParams.temperature) { mutableStateOf(s.chatParams.temperature) }
    var maxTokens by remember(s.chatParams.maxTokens) { mutableStateOf(s.chatParams.maxTokens) }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ============ ACCOUNT ============
        SectionCard(title = "Account") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.userName.ifBlank { "Signed in" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(s.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            TextButton(onClick = { loginVm.logout() }) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sign out")
            }
        }

        // ============ PLATFORM URL (advanced) ============
        SectionCard(title = "Platform URL") {
            OutlinedTextField(
                value = s.platformUrl,
                onValueChange = { vm.updatePlatformUrl(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Platform URL") },
                singleLine = true,
                readOnly = true,
                supportingText = { Text("Where the Pullarao AppForge backend is deployed.") }
            )
        }

        // ============ MODEL ============
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
                label = { Text("System prompt (optional)") },
                minLines = 2
            )
        }

        // ============ GENERATION ============
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = s.chatParams.streaming,
                    onCheckedChange = { vm.updateChatParams(s.chatParams.copy(streaming = it)) }
                )
                Spacer(Modifier.width(8.dp))
                Text("Stream responses (SSE)", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ============ APPEARANCE ============
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

        // ============ ABOUT ============
        SectionCard(title = "About") {
            Text("Pullarao 1", style = MaterialTheme.typography.titleSmall)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Powered by Pullarao 1 open source · No API key needed for students", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
