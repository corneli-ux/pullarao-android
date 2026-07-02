package com.glm.aiapp.data.repository

import android.util.Base64
import com.glm.aiapp.data.api.PlatformClient
import com.glm.aiapp.data.db.AppDatabase
import com.glm.aiapp.data.db.TranscriptionEntity
import com.glm.aiapp.data.db.VoiceClipEntity
import com.glm.aiapp.domain.model.*
import com.glm.aiapp.domain.repository.SettingsRepository
import com.glm.aiapp.domain.repository.SpeechRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS/ASR — routed through the platform's speech proxy endpoints (see
 * PlatformClient), not called directly against Zhipu. This keeps the
 * GLM_API_KEY server-side, matching how vision/image/search already work.
 */
@Singleton
class SpeechRepositoryImpl @Inject constructor(
    private val platform: PlatformClient,
    private val db: AppDatabase,
    private val settingsRepo: SettingsRepository
) : SpeechRepository {

    override suspend fun synthesize(
        text: String,
        voice: Voice,
        speed: Float,
        format: AudioFormat
    ): VoiceClip {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        val base64 = platform.synthesizeSpeech(s.platformUrl, s.sessionToken, text, voice.id, speed, format.id)
        if (base64.isBlank()) error("Text-to-speech returned no audio")
        val entity = VoiceClipEntity(
            id = UUID.randomUUID().toString(),
            text = text,
            voice = voice.id,
            speed = speed,
            format = format.id,
            audioBase64 = base64,
            audioUrl = null,
            createdAt = System.currentTimeMillis()
        )
        db.voiceClipDao().upsert(entity)
        return entity.toDomain()
    }

    override suspend fun transcribe(audioBase64: String, fileName: String): Transcription {
        val s = settingsRepo.settings.first()
        if (s.sessionToken.isBlank()) error("Not signed in. Open Settings → Account → Sign in.")
        val bytes = Base64.decode(audioBase64, Base64.NO_WRAP)
        val text = platform.transcribeSpeech(s.platformUrl, s.sessionToken, bytes, fileName)
        val entity = TranscriptionEntity(
            id = UUID.randomUUID().toString(),
            text = text,
            fileName = fileName,
            durationMs = null,
            createdAt = System.currentTimeMillis()
        )
        db.transcriptionDao().upsert(entity)
        return entity.toDomain()
    }

    override fun observeClips() =
        db.voiceClipDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTranscriptions() =
        db.transcriptionDao().observeAll().map { list -> list.map { it.toDomain() } }

    private fun VoiceClipEntity.toDomain() = VoiceClip(
        id = id, text = text, voice = voice, speed = speed, format = format,
        audioBase64 = audioBase64, audioUrl = audioUrl, createdAt = createdAt
    )

    private fun TranscriptionEntity.toDomain() = Transcription(
        id = id, text = text, fileName = fileName, durationMs = durationMs, createdAt = createdAt
    )
}
