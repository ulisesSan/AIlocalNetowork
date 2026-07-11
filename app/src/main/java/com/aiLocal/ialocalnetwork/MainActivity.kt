package com.aiLocal.ialocalnetwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiLocal.ialocalnetwork.ui.theme.IALocalNetworkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ollamaClient = IAClient()
        val configManager = ConfigManager(this)
        val db = ChatDatabase.getDatabase(this)
        val chatDao = db.chatDao()

        setContent {
            IALocalNetworkTheme(dynamicColor = false) {
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                // --- ESTADOS DE CHAT Y DB ---
                var currentConversationId by remember { mutableStateOf<Long?>(null) }
                val conversations by chatDao.getAllConversations().collectAsState(initial = emptyList())
                val messages by (currentConversationId?.let { 
                    chatDao.getMessagesForConversation(it)
                } ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

                var prompt by remember { mutableStateOf("") }
                var streamingResponse by remember { mutableStateOf("") }
                var isGenerating by remember { mutableStateOf(false) }
                var chatContext by remember { mutableStateOf<List<Int>?>(null) }

                // --- ESTADOS DE CONFIGURACIÓN ---
                var models by remember { mutableStateOf(listOf<String>()) }
                var selectedModel by remember { mutableStateOf("Cargando...") }
                var expanded by remember { mutableStateOf(false) }
                var showConfigDialog by remember { mutableStateOf(false) }
                var currentIp by remember { mutableStateOf(configManager.getIp()) }
                var ipInput by remember { mutableStateOf(currentIp) }

                // --- ESTADOS DE DESCARGA ---
                var showDownloadDialog by remember { mutableStateOf(false) }
                var modelToDownload by remember { mutableStateOf("") }
                var downloadProgress by remember { mutableStateOf<IAClient.DownloadProgress?>(null) }

                val listState = rememberLazyListState()

                // Scroll automático al final cuando llegan mensajes o hay streaming
                LaunchedEffect(messages.size, streamingResponse) {
                    if (messages.isNotEmpty() || streamingResponse.isNotEmpty()) {
                        listState.animateScrollToItem((messages.size + if (streamingResponse.isNotEmpty()) 1 else 0))
                    }
                }

                // Carga inicial de modelos
                LaunchedEffect(currentIp) {
                    val loadedModels = ollamaClient.getLocalModels(currentIp)
                    if (loadedModels.isNotEmpty()) {
                        models = loadedModels
                        if (selectedModel == "Cargando..." || selectedModel == "Ninguno") {
                            selectedModel = loadedModels[0]
                        }
                    } else {
                        models = listOf("No se encontraron modelos")
                        selectedModel = "Ninguno"
                    }
                }

                // --- COMPONENTES DE UI ---

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Conversaciones",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            NavigationDrawerItem(
                                label = { Text("Nueva Charla") },
                                selected = currentConversationId == null,
                                onClick = {
                                    currentConversationId = null
                                    chatContext = null
                                    streamingResponse = ""
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(conversations) { conv ->
                                    NavigationDrawerItem(
                                        label = { Text(conv.title, maxLines = 1) },
                                        selected = conv.id == currentConversationId,
                                        onClick = {
                                            currentConversationId = conv.id
                                            streamingResponse = ""
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("IA Local") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { showConfigDialog = true }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .imePadding()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Selector de Modelo
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Modelo") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    models.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                selectedModel = model
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Botón descarga y progreso
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showDownloadDialog = true }) {
                                    Icon(Icons.Default.Download, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Descargar")
                                }
                            }

                            downloadProgress?.let { progress ->
                                LinearProgressIndicator(
                                    progress = { progress.percentage / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )
                            }

                            // Área de Chat
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(messages) { msg ->
                                    ChatBubble(msg.role, msg.content)
                                }
                                if (streamingResponse.isNotEmpty()) {
                                    item { ChatBubble("assistant", streamingResponse) }
                                }
                            }

                            // Input
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = prompt,
                                    onValueChange = { prompt = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Escribe algo...") },
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4
                                )
                                Spacer(Modifier.width(8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        if (prompt.isNotBlank() && !isGenerating) {
                                            val userPrompt = prompt
                                            prompt = ""
                                            isGenerating = true
                                            
                                            scope.launch(Dispatchers.IO) {
                                                // 1. Asegurar conversación
                                                var convId = currentConversationId
                                                if (convId == null) {
                                                    convId = chatDao.insertConversation(
                                                        Conversation(title = userPrompt.take(20) + "...")
                                                    )
                                                    currentConversationId = convId
                                                }

                                                // 2. Guardar mensaje usuario
                                                chatDao.insertMessage(
                                                    ChatMessage(conversationId = convId, role = "user", content = userPrompt)
                                                )

                                                // 3. Streaming de Ollama
                                                var fullResponse = ""
                                                ollamaClient.askOllamaStreaming(
                                                    prompt = userPrompt,
                                                    ip = currentIp,
                                                    model = selectedModel,
                                                    context = chatContext
                                                ).collect { chunk ->
                                                    if (chunk.response.isNotEmpty()) {
                                                        fullResponse += chunk.response
                                                        streamingResponse = fullResponse
                                                    }
                                                    if (chunk.done) {
                                                        chatContext = chunk.context
                                                        // Guardar respuesta final en DB
                                                        chatDao.insertMessage(
                                                            ChatMessage(conversationId = convId, role = "assistant", content = fullResponse)
                                                        )
                                                        streamingResponse = ""
                                                        isGenerating = false
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    if (isGenerating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // --- DIÁLOGOS (Config e IP) ---
                if (showConfigDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfigDialog = false },
                        title = { Text("Configuración") },
                        text = {
                            OutlinedTextField(
                                value = ipInput,
                                onValueChange = { ipInput = it },
                                label = { Text("IP Ollama") }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                configManager.saveIp(ipInput)
                                currentIp = ipInput
                                showConfigDialog = false
                            }) { Text("Guardar") }
                        }
                    )
                }

                if (showDownloadDialog) {
                    AlertDialog(
                        onDismissRequest = { showDownloadDialog = false },
                        title = { Text("Descargar Modelo") },
                        text = {
                            OutlinedTextField(
                                value = modelToDownload,
                                onValueChange = { modelToDownload = it },
                                label = { Text("Nombre (ej: llama3)") }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    ollamaClient.pullModel(currentIp, modelToDownload).collect { downloadProgress = it }
                                }
                                showDownloadDialog = false
                            }) { Text("Descargar") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(role: String, content: String) {
    val isUser = role == "user"
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
