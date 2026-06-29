package com.glm.aiapp.data.repository

import com.glm.aiapp.data.api.PlatformClient
import com.glm.aiapp.domain.model.Attachment
import com.glm.aiapp.domain.repository.SettingsRepository
import com.glm.aiapp.domain.repository.VisionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisionRepositoryImpl @Inject constructor(
    private val platform: PlatformClient,
    private val settingsRepo: SettingsRepository
) : VisionRepository {

    override suspend fun analyze(prompt: String, attachments: List<Attachment>, thinking: Boolean): String {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        if (attachments.isEmpty()) error("Pick an image first")
        // The platform proxy currently expects a single image URL. Take the first image attachment.
        val imageUrl = attachments.firstOrNull()?.url
            ?: error("No image attachment")
        return platform.vision(s.platformUrl, s.sessionToken, prompt, imageUrl)
    }
}
