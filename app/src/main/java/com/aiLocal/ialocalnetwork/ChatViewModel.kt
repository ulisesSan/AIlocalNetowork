package com.aiLocal.ialocalnetwork

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.aiLocal.ialocalnetwork.IAClient
import com.aiLocal.ialocalnetwork.ConfigManager

class ChatViewModel(
    private val ollamaClient: IAClient,
    private val configManager: ConfigManager
) : ViewModel() {

    // Estados de la UI
    var prompt by mutableStateOf("")
    var lastPrompt by mutableStateOf("")
    var responseText by mutableStateOf("La respuesta aparecerá aquí...")
    var selectedModel by mutableStateOf("Cargando...")
    var models by mutableStateOf(listOf<String>())
    var currentIp by mutableStateOf(configManager.getIp())
    var showDialog by mutableStateOf(false)

    init {
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            val fetched = ollamaClient.getLocalModels(currentIp)
            if (fetched.isNotEmpty() && !fetched.contains("Error al cargar")) {
                models = fetched
                selectedModel = fetched[0]
            }
        }
    }

    fun updateIp(newIp: String) {
        configManager.saveIp(newIp)
        currentIp = newIp
        loadModels() // Recargar modelos al cambiar IP
    }

    fun sendPrompt() {
        if (prompt.isNotBlank()) {
            viewModelScope.launch {
                lastPrompt = prompt
                val promptToSend = prompt
                prompt = ""
                responseText = "Pensando..."
                val fullJson = ollamaClient.askOllama(promptToSend, currentIp)
                responseText = parseOllamaResponse(fullJson)
            }
        }
    }

    // Tu función de Regex ahora vive aquí
    private fun parseOllamaResponse(rawJson: String): String {
        val regex = "\"response\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
        val match = regex.find(rawJson)
        return match?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: "No se pudo procesar la respuesta"
    }
}