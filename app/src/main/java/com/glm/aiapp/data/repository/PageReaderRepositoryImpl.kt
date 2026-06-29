package com.glm.aiapp.data.repository

import com.glm.aiapp.data.api.PlatformClient
import com.glm.aiapp.domain.model.PageReadResult
import com.glm.aiapp.domain.repository.PageReaderRepository
import com.glm.aiapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageReaderRepositoryImpl @Inject constructor(
    private val platform: PlatformClient,
    private val settingsRepo: SettingsRepository
) : PageReaderRepository {

    override suspend fun read(url: String): PageReadResult {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        val result = platform.pageReader(s.platformUrl, s.sessionToken, url)
        return PageReadResult(
            title = result.title,
            url = result.url,
            html = result.html,
            publishedTime = result.publishedTime,
            tokens = result.tokens
        )
    }
}
