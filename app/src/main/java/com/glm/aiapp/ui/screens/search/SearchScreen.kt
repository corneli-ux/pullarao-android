package com.glm.aiapp.ui.screens.search

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.ui.components.EmptyState
import com.glm.aiapp.ui.components.LoadingState

@Composable
fun SearchScreen(vm: SearchViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search the web") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )
        Button(
            onClick = vm::search,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.query.isNotBlank()
        ) { Text(if (state.isLoading) "Searching…" else "Search") }

        state.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        if (state.isLoading) {
            LoadingState(message = "Searching the web…")
        } else if (state.results.isEmpty() && state.query.isBlank()) {
            EmptyState(
                title = "Search anything",
                subtitle = "Powered by Pullarao — get fresh results with snippets and sources."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(state.results, key = { it.url }) { result ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url))) }
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(result.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
                            if (result.source != null || result.publishedDate != null) {
                                Text(
                                    listOfNotNull(result.source, result.publishedDate).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(result.snippet, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(result.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
