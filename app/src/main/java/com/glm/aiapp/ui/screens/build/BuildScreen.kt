package com.glm.aiapp.ui.screens.build

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.ui.components.EmptyState
import com.glm.aiapp.ui.components.LoadingState

@Composable
fun BuildScreen(vm: BuildViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        vm.loadProjects()
        vm.loadGithubStatus()
        vm.loadDeployTargets()
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // If a project is selected, show detail
        if (state.activeProject != null) {
            ProjectDetailView(
                project = state.activeProject!!,
                files = state.activeProjectFiles,
                githubConnected = state.githubConnected,
                deployTargets = state.deployTargets,
                isPushing = state.isPushing,
                isDeploying = state.isDeploying,
                onPush = { vm.pushToGithub(state.activeProject!!.id) },
                onDeploy = { p -> vm.deploy(state.activeProject!!.id, p) },
                onBack = { vm.clearActiveProject() }
            )
            return@Column
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Projects") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Connections") })
        }

        when (activeTab) {
            0 -> ProjectsTab(state, vm)
            1 -> ConnectionsTab(state, vm)
        }
    }
}

@Composable
private fun ProjectsTab(state: BuildUiState, vm: BuildViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // New project form
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Build a new app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.newName, onValueChange = vm::setName,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Project name") }, singleLine = true
                )
                OutlinedTextField(
                    value = state.newDescription, onValueChange = vm::setDescription,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Describe what the app should do") }, minLines = 3, maxLines = 5
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WEB_APP" to "Web", "ANDROID_APP" to "Android", "STATIC_SITE" to "Static").forEach { (id, label) ->
                        FilterChip(
                            selected = state.newAppType == id,
                            onClick = { vm.setAppType(id) },
                            label = { Text(label) }
                        )
                    }
                }
                Button(
                    onClick = vm::generateProject,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isGenerating && state.newName.isNotBlank() && state.newDescription.isNotBlank()
                ) {
                    if (state.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Generating with Pullarao 1…")
                    } else {
                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate with Pullarao 1")
                    }
                }
            }
        }

        // Project list
        Text("Your projects", style = MaterialTheme.typography.titleMedium)
        if (state.isLoading) {
            LoadingState(message = "Loading projects…")
        } else if (state.projects.isEmpty()) {
            EmptyState(title = "No projects yet", subtitle = "Describe an app above and Pullarao 1 will generate it.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.projects, key = { it.id }) { p ->
                    ProjectCard(p, onClick = { vm.loadProjectDetail(p.id) })
                }
            }
        }

        state.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProjectCard(p: ProjectSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(p.status, style = MaterialTheme.typography.labelSmall) })
            }
            Text("${p.appType.replace("_", " ")} · ${p.framework ?: ""} · ${p.fileCount} files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.githubRepoUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pushed to GitHub", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (p.deployUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Rocket, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deployed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailView(
    project: ProjectSummary,
    files: List<ProjectFile>,
    githubConnected: Boolean,
    deployTargets: List<String>,
    isPushing: Boolean,
    isDeploying: Boolean,
    onPush: () -> Unit,
    onDeploy: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(8.dp))
            Text(project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text("${project.appType.replace("_", " ")} · ${project.framework ?: ""} · ${project.fileCount} files · ${project.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPush, enabled = !isPushing && githubConnected && project.fileCount > 0, modifier = Modifier.weight(1f)) {
                if (isPushing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isPushing) "Pushing…" else if (project.githubRepoUrl != null) "Re-push" else "Push to GitHub")
            }
            if (project.appType != "ANDROID_APP") {
                deployTargets.forEach { p ->
                    Button(onClick = { onDeploy(p) }, enabled = !isDeploying, modifier = Modifier.weight(1f)) {
                        if (isDeploying) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Filled.Rocket, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deploy")
                    }
                }
            }
        }
        if (!githubConnected) {
            Text("Connect your GitHub in the Connections tab first to push code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (project.appType != "ANDROID_APP" && deployTargets.isEmpty()) {
            Text("Connect a deploy target in the Connections tab first to deploy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        // Links
        project.githubRepoUrl?.let {
            Text("GitHub: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        project.deployUrl?.let {
            Text("Live: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        // Files
        Text("Generated files (${files.size})", style = MaterialTheme.typography.titleMedium)
        files.forEach { f ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text(f.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(f.content.take(500), style = MaterialTheme.typography.bodySmall, maxLines = 10, overflow = TextOverflow.Ellipsis)
                    if (f.content.length > 500) Text("…(${f.content.length} chars)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ConnectionsTab(state: BuildUiState, vm: BuildViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // GitHub
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.githubConnected) {
                    Text("Connected as @${state.githubLogin ?: "user"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Paste a Personal Access Token (scopes: repo, workflow). Get one at github.com/settings/tokens", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.githubPat, onValueChange = vm::setGithubPat,
                        modifier = Modifier.fillMaxWidth(), label = { Text("GitHub PAT") }, singleLine = true
                    )
                    Button(onClick = vm::connectGithub, enabled = state.githubPat.isNotBlank()) {
                        Text("Connect GitHub")
                    }
                }
            }
        }

        // Deploy targets
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Deploy targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.deployTargets.isNotEmpty()) {
                    state.deployTargets.forEach { t ->
                        Text("✓ $t connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider()
                Text("Add a deploy target", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VERCEL" to "Vercel", "NETLIFY" to "Netlify", "CLOUDFLARE_PAGES" to "Cloudflare").forEach { (id, label) ->
                        FilterChip(selected = state.deployProvider == id, onClick = { vm.setDeployProvider(id) }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = state.deployToken, onValueChange = vm::setDeployToken,
                    modifier = Modifier.fillMaxWidth(), label = { Text("API token") }, singleLine = true
                )
                Button(onClick = vm::connectDeploy, enabled = state.deployToken.isNotBlank()) {
                    Text("Connect ${state.deployProvider}")
                }
            }
        }
    }
}
