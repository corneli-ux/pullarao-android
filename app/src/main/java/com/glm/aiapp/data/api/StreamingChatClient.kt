package com.glm.aiapp.data.api

import com.glm.aiapp.data.dto.StreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingChatClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) {
    fun stream(url: String, apiKey: String, payload: String): Flow<StreamChunk> = channelFlow {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        try {
            call.execute().use { response: Response ->
                if (!response.isSuccessful) {
                    close(ServerSentEventException("HTTP ${response.code}"))
                    return@use
                }
                val body = response.body ?: run {
                    close(ServerSentEventException("Empty body"))
                    return@use
                }
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var line = reader.readLine()
                while (line != null && !isClosedForSend) {
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data != "[DONE]") {
                            val chunk = runCatching { json.decodeFromString<StreamChunk>(data) }.getOrNull()
                            if (chunk != null) send(chunk)
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (t: Throwable) {
            close(t)
        }
    }.flowOn(Dispatchers.IO)

    fun streamAuthorized(url: String, sessionToken: String, payload: String): Flow<org.json.JSONObject> = channelFlow {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $sessionToken")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        try {
            call.execute().use { response: Response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()?.take(300) ?: response.message
                    close(ServerSentEventException("HTTP ${response.code}: $errBody"))
                    return@use
                }
                val body = response.body ?: run {
                    close(ServerSentEventException("Empty body"))
                    return@use
                }
                // Use BufferedReader on the raw InputStream — bypasses OkHttp's
                // source/buffer layer which can hold partial data without flushing
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var line = reader.readLine()
                while (line != null && !isClosedForSend) {
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data != "[DONE]") {
                            val obj = runCatching { org.json.JSONObject(data) }.getOrNull()
                            if (obj != null) send(obj)
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (t: Throwable) {
            close(t)
        }
    }.flowOn(Dispatchers.IO)
}

class ServerSentEventException(message: String) : RuntimeException(message)
