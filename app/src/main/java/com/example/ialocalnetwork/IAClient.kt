package com.example.ialocalnetwork

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class IAClient {
    private val client = HttpClient()

    suspend fun askOllama(prompt: String): String {
        return try {
            // Construimos el JSON como un String plano
            val escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n")
            val jsonBody = """
                {
                    "model": "llama3.1",
                    "prompt": "$escapedPrompt",
                    "stream": false,
                    "keep_alive": 0,
                    "options": {
                        "num_predict": 8192
                    }
                }
            """.trimIndent()

            val response = client.post("http://192.168.1.81:11434/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }

            // Devolvemos el cuerpo completo (será un JSON en String)
            response.bodyAsText()
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    suspend fun getLocalModels(): List<String> {
        return try {
            val response = client.get("http://192.168.1.81:11434/api/tags") // Usa tu IP real
            val body = response.bodyAsText()

            // Regex rápida para sacar los nombres de los modelos
            val regex = "\"name\"\\s*:\\s*\"(.*?)\"".toRegex()
            regex.findAll(body).map { it.groupValues[1] }.toList()
        } catch (e: Exception) {
            listOf("Error al cargar")
        }
    }
}