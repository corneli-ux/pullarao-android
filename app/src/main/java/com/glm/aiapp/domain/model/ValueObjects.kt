package com.glm.aiapp.domain.model

/**
 * Value objects — typed primitives for settings & API params.
 */

enum class ChatModel(val id: String, val label: String, val description: String) {
    GLM_4_6("glm-4.6", "Pullarao 1", "Latest flagship — best reasoning, multimodal-aware"),
    GLM_4_5("glm-4.5", "GLM-4.5", "Balanced quality and speed"),
    GLM_4_PLUS("glm-4-plus", "GLM 4 Plus", "Long-context, stable for production"),
    GLM_4_AIR("glm-4-air", "GLM 4 Air", "Cost-efficient, high-throughput"),
    GLM_4_FLASH("glm-4-flash", "GLM 4 Flash", "Fastest inference, ideal for chat");

    companion object {
        fun fromId(id: String?): ChatModel = entries.firstOrNull { it.id == id } ?: GLM_4_6
    }
}

enum class ImageSize(val id: String, val label: String, val ratio: String) {
    SQUARE("1024x1024", "Square 1:1", "1 / 1"),
    PORTRAIT_3_5("768x1344", "Portrait 3:5", "3 / 5"),
    PORTRAIT_3_4("864x1152", "Portrait 3:4", "3 / 4"),
    LANDSCAPE_5_3("1344x768", "Landscape 5:3", "5 / 3"),
    LANDSCAPE_4_3("1152x864", "Landscape 4:3", "4 / 3"),
    WIDE("1440x720", "Wide 2:1", "2 / 1"),
    TALL("720x1440", "Tall 1:2", "1 / 2");

    companion object {
        fun fromId(id: String?): ImageSize = entries.firstOrNull { it.id == id } ?: SQUARE
    }
}

enum class VideoSize(val id: String, val label: String) {
    HD_LANDSCAPE("1920x1080", "HD 1920x1080"),
    HD_PORTRAIT("1080x1920", "HD Portrait 1080x1920"),
    SD_LANDSCAPE("1280x720", "SD 1280x720"),
    SD_PORTRAIT("720x1280", "SD Portrait 720x1280")
}

enum class VideoQuality(val id: String, val label: String) {
    SPEED("speed", "Speed — fastest generation"),
    QUALITY("quality", "Quality — best fidelity")
}

enum class Voice(val id: String, val label: String) {
    TONGTONG("tongtong", "Tongtong (default)"),
    MALE("male", "Male voice"),
    FEMALE("female", "Female voice")
}

enum class AudioFormat(val id: String, val label: String) {
    WAV("wav", "WAV"),
    PCM("pcm", "PCM (streaming)"),
    MP3("mp3", "MP3")
}

enum class ThinkingMode(val id: String, val label: String) {
    ENABLED("enabled", "Enabled — show reasoning"),
    DISABLED("disabled", "Disabled — direct answers")
}

data class ChatParams(
    val model: ChatModel = ChatModel.GLM_4_6,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val topP: Float = 0.9f,
    val thinking: ThinkingMode = ThinkingMode.DISABLED,
    val systemPrompt: String = "",
    val streaming: Boolean = true
)

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = DEFAULT_BASE_URL,
    val chatParams: ChatParams = ChatParams(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
