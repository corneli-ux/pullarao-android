package com.glm.aiapp.ui.screens.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.ui.components.EmptyState
import com.glm.aiapp.ui.components.LoadingState

@Composable
fun ReaderScreen(vm: ReaderViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.url,
            onValueChange = vm::setUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Page URL") },
            leadingIcon = { Icon(Icons.Filled.Article, contentDescription = null) },
            singleLine = true
        )
        Button(
            onClick = vm::read,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.url.isNotBlank()
        ) { Text(if (state.isLoading) "Reading…" else "Read page") }

        state.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        if (state.isLoading) {
            LoadingState(message = "Pullarao is reading the page…")
        } else if (state.result == null) {
            EmptyState(
                title = "Read any web page",
                subtitle = "Paste a URL and Pullarao fetches the HTML, extracts the title and main content."
            )
        } else {
            val r = state.result!!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(r.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) } }
                    ) { Text(r.url, style = MaterialTheme.typography.labelSmall) }
                    if (r.publishedTime != null) {
                        Text("Published: ${r.publishedTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (r.tokens != null) {
                        Text("Tokens: ${r.tokens}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text("Extracted HTML (raw)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(r.html.take(5000), style = MaterialTheme.typography.bodySmall)
                    if (r.html.length > 5000) {
                        Spacer(Modifier.height(8.dp))
                        Text("…truncated. Full HTML saved in result.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
