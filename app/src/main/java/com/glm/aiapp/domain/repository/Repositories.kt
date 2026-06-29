package com.glm.aiapp.domain.repository

import com.glm.aiapp.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts — interfaces defined in domain, implemented in data.
 * Use cases depend on these abstractions, never on concrete Retrofit / Room.
 */

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversation(id: String): Flow<Conversation?>
    suspend fun createConversation(title: String, systemPrompt: String, model: String): Conversation
    suspend fun deleteConversation(id: String)
    suspend fun appendMessage(conversationId: String, message: Message)
    suspend fun updateMessage(message: Message)

    /** Streams assistant tokens as they arrive. Emits the final assembled message on completion. */
    suspend fun streamChat(
        conversationId: String,
        params: ChatParams,
        onToken: (String) -> Unit,
        onThinking: (String) -> Unit
    ): Message
}

interface VisionRepository {
    suspend fun analyze(prompt: String, attachments: List<Attachment>, thinking: Boolean): String
}

interface ImageRepository {
    suspend fun generate(prompt: String, size: ImageSize): GeneratedImage
    fun observeGallery(): Flow<List<GeneratedImage>>
    suspend fun deleteImage(id: String)
}

interface VideoRepository {
    suspend fun createTask(prompt: String, quality: VideoQuality, size: VideoSize, fps: Int, duration: Int): GeneratedVideo
    suspend fun pollTask(taskId: String): GeneratedVideo
    fun observeVideos(): Flow<List<GeneratedVideo>>
    suspend fun deleteVideo(id: String)
}

interface SpeechRepository {
    suspend fun synthesize(text: String, voice: Voice, speed: Float, format: AudioFormat): VoiceClip
    suspend fun transcribe(audioBase64: String, fileName: String): Transcription
    fun observeClips(): Flow<List<VoiceClip>>
    fun observeTranscriptions(): Flow<List<Transcription>>
}

interface SearchRepository {
    suspend fun search(query: String, num: Int, recencyDays: Int): List<SearchResult>
}

interface PageReaderRepository {
    suspend fun read(url: String): PageReadResult
}

interface FineTuneRepository {
    fun observeDatasets(): Flow<List<FineTuneDataset>>
    fun observeJobs(): Flow<List<FineTuneJob>>
    suspend fun createJob(name: String, baseModel: String, datasetId: String, epochs: Int, learningRate: Float): FineTuneJob
    suspend fun refreshJobStatus(id: String): FineTuneJob
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updatePlatformUrl(url: String)
    suspend fun setSession(token: String, email: String, name: String)
    suspend fun clearSession()
    suspend fun updateChatParams(params: ChatParams)
    suspend fun updateThemeMode(mode: ThemeMode)
}
