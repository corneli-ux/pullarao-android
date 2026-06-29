package com.glm.aiapp.data.repository

import com.glm.aiapp.data.api.PlatformClient
import com.glm.aiapp.domain.model.SearchResult
import com.glm.aiapp.domain.repository.SearchRepository
import com.glm.aiapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val platform: PlatformClient,
    private val settingsRepo: SettingsRepository
) : SearchRepository {

    override suspend fun search(query: String, num: Int, recencyDays: Int): List<SearchResult> {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        val hits = platform.webSearch(s.platformUrl, s.sessionToken, query, num, recencyDays)
        return hits.map {
            SearchResult(
                title = it.title,
                url = it.url,
                snippet = it.snippet,
                source = it.source,
                publishedDate = it.publishedDate
            )
        }
    }
}
