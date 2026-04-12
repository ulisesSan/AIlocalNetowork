package com.aiLocal.ialocalnetwork

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfigDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var ipInput by remember { mutableStateOf(currentIp) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Servidor") },
        text = {
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("IP de tu PC (Ollama)") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(ipInput) }) { Text("Guardar") }
        }
    )
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Surface(
        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(0.85f)
            .padding(
                start = if (isUser) 40.dp else 0.dp,
                end = if (isUser) 0.dp else 40.dp
            )
    ) {
        Text(text = text, modifier = Modifier.padding(12.dp))
    }
}
