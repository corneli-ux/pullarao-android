package com.glm.aiapp.ui.screens.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glm.aiapp.data.api.PlatformClient
import com.glm.aiapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ProjectSummary(
    val id: String,
    val name: String,
    val appType: String,
    val status: String,
    val framework: String?,
    val fileCount: Int,
    val githubRepoUrl: String?,
    val deployUrl: String?
)

data class BuildUiState(
    val projects: List<ProjectSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isPushing: Boolean = false,
    val isDeploying: Boolean = false,
    val error: String? = null,
    // New project form
    val newName: String = "",
    val newDescription: String = "",
    val newAppType: String = "WEB_APP",
    // GitHub connection
    val githubConnected: Boolean = false,
    val githubLogin: String? = null,
    val githubPat: String = "",
    // Deploy targets
    val deployTargets: List<String> = emptyList(),
    val deployProvider: String = "VERCEL",
    val deployToken: String = "",
    // Active project detail
    val activeProject: ProjectSummary? = null,
    val activeProjectFiles: List<ProjectFile> = emptyList(),
    val activeProjectLog: String? = null
)

data class ProjectFile(val path: String, val content: String, val language: String?)

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val platform: PlatformClient
) : ViewModel() {

    private val _state = MutableStateFlow(BuildUiState())
    val state: StateFlow<BuildUiState> = _state

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    fun setName(v: String) { _state.value = _state.value.copy(newName = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(newDescription = v) }
    fun setAppType(v: String) { _state.value = _state.value.copy(newAppType = v) }
    fun setGithubPat(v: String) { _state.value = _state.value.copy(githubPat = v) }
    fun setDeployProvider(v: String) { _state.value = _state.value.copy(deployProvider = v) }
    fun setDeployToken(v: String) { _state.value = _state.value.copy(deployToken = v) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    private suspend fun getAuth(): Pair<String, String> {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        return s.platformUrl.trimEnd('/') to s.sessionToken
    }

    fun loadProjects() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/projects")
                            .header("Authorization", "Bearer $token")
                            .get().build()
                    ).execute().use { it.body?.string() ?: "[]" }
                }
                val json = try { JSONObject(res) } catch (_: Exception) { JSONObject().put("project", JSONObject()) }
                val arr = json.optJSONArray("projects") ?: JSONArray()
                val projects = (0 until arr.length()).map { i ->
                    val p = arr.getJSONObject(i)
                    ProjectSummary(
                        id = p.getString("id"),
                        name = p.getString("name"),
                        appType = p.getString("appType"),
                        status = p.getString("status"),
                        framework = p.optString("framework", null),
                        fileCount = p.optInt("fileCount"),
                        githubRepoUrl = p.optString("githubRepoUrl", null),
                        deployUrl = p.optString("deployUrl", null)
                    )
                }
                _state.value = _state.value.copy(projects = projects, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadGithubStatus() {
        viewModelScope.launch {
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/settings/github")
                            .header("Authorization", "Bearer $token")
                            .get().build()
                    ).execute().use { it.body?.string() ?: "{}" }
                }
                val json = try { JSONObject(res) } catch (_: Exception) { JSONObject().put("project", JSONObject()) }
                _state.value = _state.value.copy(
                    githubConnected = json.optBoolean("connected", false),
                    githubLogin = json.optString("githubLogin", null)
                )
            } catch (_: Exception) { }
        }
    }

    fun loadDeployTargets() {
        viewModelScope.launch {
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/settings/deploy")
                            .header("Authorization", "Bearer $token")
                            .get().build()
                    ).execute().use { it.body?.string() ?: "{}" }
                }
                val json = try { JSONObject(res) } catch (_: Exception) { JSONObject().put("project", JSONObject()) }
                val arr = json.optJSONArray("targets") ?: JSONArray()
                val targets = (0 until arr.length()).map { arr.getJSONObject(it).getString("provider") }
                _state.value = _state.value.copy(deployTargets = targets)
            } catch (_: Exception) { }
        }
    }

    fun connectGithub() {
        val pat = _state.value.githubPat.trim()
        if (pat.isBlank()) { _state.value = _state.value.copy(error = "Paste your GitHub PAT first"); return }
        viewModelScope.launch {
            try {
                val (url, token) = getAuth()
                withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/settings/github")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(JSONObject().put("pat", pat).toString().toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { r -> if (!r.isSuccessful) { val b = r.body?.string() ?: ""; throw RuntimeException("HTTP ${r.code}: ${b.take(100)}") } }
                }
                _state.value = _state.value.copy(githubPat = "")
                loadGithubStatus()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun connectDeploy() {
        val t = _state.value.deployToken.trim()
        if (t.isBlank()) { _state.value = _state.value.copy(error = "Paste your deploy token first"); return }
        viewModelScope.launch {
            try {
                val (url, token) = getAuth()
                withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/settings/deploy")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(JSONObject()
                                .put("provider", _state.value.deployProvider)
                                .put("token", t)
                                .toString()
                                .toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { r -> if (!r.isSuccessful) { val b = r.body?.string() ?: ""; throw RuntimeException("HTTP ${r.code}: ${b.take(100)}") } }
                }
                _state.value = _state.value.copy(deployToken = "")
                loadDeployTargets()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun generateProject() {
        val s = _state.value
        if (s.newName.isBlank() || s.newDescription.isBlank()) {
            _state.value = s.copy(error = "Name and description are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isGenerating = true, error = null)
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/projects")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(JSONObject()
                                .put("name", s.newName)
                                .put("description", s.newDescription)
                                .put("appType", s.newAppType)
                                .toString()
                                .toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { r ->
                        val body = r.body?.string() ?: ""
                        if (!r.isSuccessful) { val msg = if (body.isNotBlank()) { try { JSONObject(body).optString("error", "HTTP ${r.code}") } catch (_: Exception) { "Server error (HTTP ${r.code})" } } else { "Server error (HTTP ${r.code}, empty response)" }; throw RuntimeException(msg) }
                        if (body.isBlank()) throw RuntimeException("Server returned empty response") else JSONObject(body)
                    }
                }
                _state.value = _state.value.copy(
                    isGenerating = false,
                    newName = "",
                    newDescription = ""
                )
                loadProjects()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isGenerating = false, error = e.message)
            }
        }
    }

    fun pushToGithub(projectId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPushing = true, error = null)
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/projects/push")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(JSONObject().put("projectId", projectId).toString().toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { r ->
                        val body = r.body?.string() ?: ""
                        if (!r.isSuccessful) { val msg = if (body.isNotBlank()) { try { JSONObject(body).optString("error", "HTTP ${r.code}") } catch (_: Exception) { "Server error (HTTP ${r.code})" } } else { "Server error (HTTP ${r.code}, empty response)" }; throw RuntimeException(msg) }
                        if (body.isBlank()) throw RuntimeException("Server returned empty response") else JSONObject(body)
                    }
                }
                _state.value = _state.value.copy(isPushing = false)
                loadProjects()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isPushing = false, error = e.message)
            }
        }
    }

    fun deploy(projectId: String, provider: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeploying = true, error = null)
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/projects/deploy")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(JSONObject()
                                .put("projectId", projectId)
                                .put("provider", provider)
                                .toString()
                                .toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { r ->
                        val body = r.body?.string() ?: ""
                        if (!r.isSuccessful) { val msg = if (body.isNotBlank()) { try { JSONObject(body).optString("error", "HTTP ${r.code}") } catch (_: Exception) { "Server error (HTTP ${r.code})" } } else { "Server error (HTTP ${r.code}, empty response)" }; throw RuntimeException(msg) }
                        if (body.isBlank()) throw RuntimeException("Server returned empty response") else JSONObject(body)
                    }
                }
                _state.value = _state.value.copy(isDeploying = false)
                loadProjects()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDeploying = false, error = e.message)
            }
        }
    }

    fun loadProjectDetail(projectId: String) {
        viewModelScope.launch {
            try {
                val (url, token) = getAuth()
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$url/api/projects/$projectId")
                            .header("Authorization", "Bearer $token")
                            .get().build()
                    ).execute().use { it.body?.string() ?: "{}" }
                }
                val json = try { JSONObject(res) } catch (_: Exception) { JSONObject().put("project", JSONObject()) }.getJSONObject("project")
                val filesArr = json.optJSONArray("files") ?: JSONArray()
                val files = (0 until filesArr.length()).map {
                    val f = filesArr.getJSONObject(it)
                    ProjectFile(f.getString("path"), f.getString("content"), f.optString("language", null))
                }
                val summary = ProjectSummary(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    appType = json.getString("appType"),
                    status = json.getString("status"),
                    framework = json.optString("framework", null),
                    fileCount = json.optInt("fileCount"),
                    githubRepoUrl = json.optString("githubRepoUrl", null),
                    deployUrl = json.optString("deployUrl", null)
                )
                _state.value = _state.value.copy(
                    activeProject = summary,
                    activeProjectFiles = files,
                    activeProjectLog = json.optString("generationLog", null)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun clearActiveProject() {
        _state.value = _state.value.copy(activeProject = null, activeProjectFiles = emptyList(), activeProjectLog = null)
    }
}
