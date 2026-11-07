package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpeechInputBar(
    onSendText: (String) -> Unit,
    onStartVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStartVoice) {
            Icon(Icons.Default.Mic, contentDescription = "Dictar por voz")
        }
        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Escriba o hable...") }
        )
        IconButton(onClick = {
            if (textState.isNotBlank()) {
                onSendText(textState)
                textState = ""
            }
        }) {
            Icon(Icons.Default.Send, contentDescription = "Enviar a avatar 3D")
        }
    }
}
