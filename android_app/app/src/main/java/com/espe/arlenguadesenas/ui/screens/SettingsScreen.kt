package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var isHighContrast by remember { mutableStateOf(true) }
    var isHapticEnabled by remember { mutableStateOf(true) }
    var isAutoTts by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "Accesibilidad y Ajustes", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Alto Contraste (WCAG AAA)", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isHighContrast, onCheckedChange = { isHighContrast = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Vibración Háptica en Detección", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isHapticEnabled, onCheckedChange = { isHapticEnabled = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lectura Automática por Voz (TTS)", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isAutoTts, onCheckedChange = { isAutoTts = it })
        }
    }
}
