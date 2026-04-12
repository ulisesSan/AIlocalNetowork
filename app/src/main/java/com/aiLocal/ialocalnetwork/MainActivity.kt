package com.aiLocal.ialocalnetwork

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiLocal.ialocalnetwork.ui.theme.IALocalNetworkTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Las clases de datos mejor aquí afuera para que sean accesibles
@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
data class OllamaResponse(
    val response: String,
    val done: Boolean
)

//class MainActivity : ComponentActivity() {
//
//    //var lastPrompt by remember { mutableStateOf("") }
//    fun parseOllamaResponse(rawJson: String): String {
//        // Busca el contenido del campo "response"
//        val regex = "\"response\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
//        val match = regex.find(rawJson)
//
//        return match?.groupValues?.get(1)
//            ?.replace("\\n", "\n")
//            ?.replace("\\\"", "\"")
//            ?: "No se pudo procesar la respuesta"
//    }
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val ollamaClient = IAClient()
//        val configManager = ConfigManager(this)
//
//        setContent {
//            IALocalNetworkTheme(dynamicColor = false) {
//
//                var lastPrompt by remember { mutableStateOf("") }
//                var prompt by remember { mutableStateOf("") }
//                var responseText by remember { mutableStateOf("La respuesta aparecerá aquí...") }
//                val scope = rememberCoroutineScope()
//                var selectedModel by remember { mutableStateOf("Configure su IP") }
//                var models by remember { mutableStateOf(listOf<String>()) }
//                var expanded by remember { mutableStateOf(false) }
//                var showDialog by remember { mutableStateOf(false) }
//                var currentIp by remember { mutableStateOf(configManager.getIp()) }
//                var ipInput by remember { mutableStateOf(currentIp) }
//
//                LaunchedEffect(Unit) {
//                    models = ollamaClient.getLocalModels(currentIp)
//                    if (models.isNotEmpty()) selectedModel = models[0]
//
//                    ollamaClient.getLocalModels(currentIp)
//                }
//
//                if (showDialog) {
//                    AlertDialog(
//                        onDismissRequest = { showDialog = false },
//                        title = { Text("Configurar Servidor") },
//                        text = {
//                            OutlinedTextField(
//                                value = ipInput,
//                                onValueChange = { ipInput = it },
//                                label = { Text("IP de tu PC (Ollama)") }
//
//                            )
//                        },
//                        confirmButton = {
//                            Button(onClick = {
//                                configManager.saveIp(ipInput)
//                                currentIp = ipInput // Actualizamos el estado local
//                                showDialog = false
//                            }) { Text("Guardar") }
//                        }
//                    )
//                }
//
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .statusBarsPadding()
//                            .padding(16.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = "Configuración de Red",
//                                style = MaterialTheme.typography.titleMedium,
//                                color = MaterialTheme.colorScheme.primary
//                            )
//                            IconButton(onClick = { showDialog = true }) {
//                                // Importante: Asegúrate de tener los iconos de Material importados
//                                Icon(
//                                    imageVector = Icons.Default.Settings,
//                                    contentDescription = "Ajustes",
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        ExposedDropdownMenuBox(
//                            expanded = expanded,
//                            onExpandedChange = { expanded = !expanded }
//                        ) {
//                            OutlinedTextField(
//                                value = selectedModel,
//                                onValueChange = {},
//                                readOnly = true,
//                                label = { Text("Selecciona IA") },
//                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                                modifier = Modifier.menuAnchor().fillMaxWidth()
//                            )
//                            ExposedDropdownMenu(
//                                expanded = expanded,
//                                onDismissRequest = { expanded = false }
//                            ) {
//                                models.forEach { model ->
//                                    DropdownMenuItem(
//                                        text = { Text(model) },
//                                        onClick = {
//                                            selectedModel = model
//                                            expanded = false
//                                        }
//                                    )
//                                }
//                            }
//                        }
//                        Column(
//                            modifier = Modifier
//                                .weight(1f)
//                                .fillMaxWidth()
//                                .verticalScroll(rememberScrollState())
//                        ) {
//                            if (lastPrompt.isNotEmpty()) {
//                                Surface(
//                                    color = MaterialTheme.colorScheme.primaryContainer,
//                                    shape = MaterialTheme.shapes.medium,
//                                    modifier = Modifier.align(Alignment.End)
//                                        .padding(start = 40.dp, bottom = 12.dp)
//                                ) {
//                                    Text(
//                                        text = lastPrompt,
//                                        modifier = Modifier.padding(12.dp),
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                                    )
//                                }
//                            }
//
//                            Surface(
//                                color = MaterialTheme.colorScheme.surfaceVariant,
//                                shape = MaterialTheme.shapes.medium,
//                                modifier = Modifier.align(Alignment.Start).padding(end = 40.dp)
//                            ) {
//                                Text(
//                                    text = responseText,
//                                    modifier = Modifier.padding(16.dp),
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(16.dp))
//
//                        OutlinedTextField(
//                            value = prompt,
//                            onValueChange = { prompt = it },
//                            modifier = Modifier.fillMaxWidth(),
//                            label = { Text("Pregúntale a tu red local") },
//                            shape = MaterialTheme.shapes.large
//                        )
//
//                        Button(
//                            onClick = {
//                                if (prompt.isNotBlank()) {
//                                    scope.launch {
//                                        lastPrompt = prompt
//                                        prompt = ""
//                                        responseText = "Pensando..."
//                                        val fullJson = ollamaClient.askOllama(lastPrompt, currentIp)
//                                        responseText = parseOllamaResponse(fullJson)
//                                    }
//                                }
//                            },
//                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
//                        ) {
//                            Text("Enviar")
//                        }
//                    }
//                }
//            }
//        }
//    }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos dependencias
        val ollamaClient = IAClient()
        val configManager = ConfigManager(this)
        val viewModel = ChatViewModel(ollamaClient, configManager)

        setContent {
            IALocalNetworkTheme(dynamicColor = false) {
                ChatScreen(viewModel)
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var expanded by remember { mutableStateOf(false) }

    if (viewModel.showDialog) {
        ConfigDialog(
            currentIp = viewModel.currentIp,
            onDismiss = { viewModel.showDialog = false },
            onSave = {
                viewModel.updateIp(it)
                viewModel.showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            // Aquí pones tu Row de configuración que ya tenías
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Dropdown de Modelos
            // ...

            // Área de Chat
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (viewModel.lastPrompt.isNotEmpty()) {
                    ChatBubble(viewModel.lastPrompt, isUser = true)
                }
                ChatBubble(viewModel.responseText, isUser = false)
            }

            // Input
            OutlinedTextField(
                value = viewModel.prompt,
                onValueChange = { viewModel.prompt = it },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { viewModel.sendPrompt() }) {
                Text("Enviar")
            }
        }
    }
}
//    class ConfigManager(context: Context) {
//        private val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
//
//        fun saveIp(ip: String) {
//            prefs.edit().putString("ollama_ip", ip).apply()
//        }
//
//        fun getIp(): String {
//            return prefs.getString("ollama_ip", "192.168.1.100") ?: "192.168.1.100"
//        }
//    }
//}