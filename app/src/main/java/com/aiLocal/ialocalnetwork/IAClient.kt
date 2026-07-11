package com.aiLocal.ialocalnetwork

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Client for interacting with the Ollama API.
 */
class IAClient {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
            // Aceptamos también el formato de streaming de Ollama aunque no lo usemos
            json(jsonConfig, contentType = ContentType("application", "x-ndjson"))
        }
    }

    // --- Data Models for Asking ---

    @Serializable
    data class AskOllamaRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean = true,
        val context: List<Int>? = null,
        val keep_alive: Int = 0,
        val options: AskOptions = AskOptions()
    )

    @Serializable
    data class AskOptions(
        val num_predict: Int = 8192
    )

    @Serializable
    data class AskOllamaResponse(
        val response: String,
        val done: Boolean,
        val context: List<Int>? = null
    )

    // --- Data Models for Models ---

    @Serializable
    data class ModelsResponse(
        val models: List<LocalModel>
    )

    @Serializable
    data class LocalModel(
        val model: String,
        val name: String,
        val size: Long,
        val digest: String,
        val details: ModelDetails? = null
    )

    @Serializable
    data class ModelDetails(
        val format: String? = null,
        val family: String? = null,
        val families: List<String>? = null,
        val parameter_size: String? = null,
        val quantization_level: String? = null
    )

    @Serializable
    data class PullModelRequest(
        val name: String
    )

    @Serializable
    data class PullModelResponse(
        val status: String? = null,
        val digest: String? = null,
        val size: Long? = null,
        val total: Long? = null,
        val applied: Long? = null,
        val version: String? = null
    )

    /**
     * Represents the current state of the model download for the UI.
     */
    data class DownloadProgress(
        val status: String,
        val percentage: Float,
        val isDone: Boolean = false,
        val error: String? = null
    )

    // --- Methods ---

    /**
     * Sends a prompt to the specified Ollama model.
     *
     * @param prompt The user prompt.
     * @param ip The IP address of the Ollama server.
     * @param model The name of the model to use.
     * @return The raw response text from Ollama.
     */
    /**
     * Sends a prompt to the specified Ollama model and receives a stream of responses.
     */
    fun askOllamaStreaming(
        prompt: String,
        ip: String,
        model: String,
        context: List<Int>? = null
    ): Flow<AskOllamaResponse> = flow {
        try {
            client.preparePost("http://$ip:11434/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(AskOllamaRequest(model = model, prompt = prompt, context = context, stream = true))
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.isBlank()) continue
                        try {
                            val chunk = jsonConfig.decodeFromString<AskOllamaResponse>(line)
                            emit(chunk)
                            if (chunk.done) break
                        } catch (e: Exception) {
                            // Skip non-json lines
                        }
                    }
                } else {
                    emit(AskOllamaResponse("Error: ${response.status}", true))
                }
            }
        } catch (e: Exception) {
            emit(AskOllamaResponse("Error: ${e.localizedMessage}", true))
        }
    }

    suspend fun askOllama(prompt: String, ip: String, model: String): String {
        return try {
            val request = AskOllamaRequest(model = model, prompt = prompt)
            val response = client.post("http://$ip:11434/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val askResponse = response.body<AskOllamaResponse>()
            askResponse.response
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Fetches the list of local models available on the Ollama server.
     *
     * @param ip The IP address of the Ollama server.
     * @return A list of model names.
     */
    suspend fun getLocalModels(ip: String): List<String> {
        return try {
            val response = client.get("http://$ip:11434/api/tags")
            val modelsResponse = response.body<ModelsResponse>()
            modelsResponse.models.map { it.name }
        } catch (e: Exception) {
            listOf("Error: ${e.localizedMessage}")
        }
    }

    /**
     * Pulls (downloads) a new model from Ollama and provides real-time progress updates.
     *
     * @param ip The IP address of the Ollama server.
     * @param modelName The name of the model to download.
     * @return A Flow of [DownloadProgress] updates to be collected by the UI.
     */
    fun pullModel(ip: String, modelName: String): Flow<DownloadProgress> = flow {
        try {
            client.preparePost("http://$ip:11434/api/pull") {
                contentType(ContentType.Application.Json)
                setBody(PullModelRequest(name = modelName))
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.isBlank()) continue

                        try {
                            val pullResponse = jsonConfig.decodeFromString<PullModelResponse>(line)
                            val status = pullResponse.status ?: "Unknown"
                            val total = pullResponse.total ?: 0L
                            val applied = pullResponse.applied ?: 0L

                            val percentage = if (total > 0) {
                                (applied.toFloat() / total.toFloat()) * 100f
                            } else {
                                0f
                            }

                            val isDone = status.equals("success", ignoreCase = true)

                            emit(DownloadProgress(
                                status = status,
                                percentage = if (isDone) 100f else percentage,
                                isDone = isDone
                            ))
                        } catch (e: Exception) {
                            // Ignore parsing errors for non-JSON stream lines
                        }
                    }
                } else {
                    emit(DownloadProgress("Error", 0f, error = "Server returned ${response.status}"))
                }
            }
        } catch (e: Exception) {
            emit(DownloadProgress("Error", 0f, error = e.localizedMessage))
        }
    }
}
