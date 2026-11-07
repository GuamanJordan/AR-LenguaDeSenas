package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.espe.arlenguadesenas.ui.components.AccessibleButton

@Composable
fun HomeScreen(
    onNavigateToSignToText: () -> Unit,
    onNavigateToTextToSign: () -> Unit,
    onNavigateToDictionary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LSEC Comunica AR",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Comunicación Bidireccional Inclusiva",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        AccessibleButton(
            text = "1. Sordo ➔ Oyente (Cámara)",
            contentDescription = "Traducir señas de la cámara a texto y voz",
            onClick = onNavigateToSignToText
        )
        Spacer(modifier = Modifier.height(16.dp))

        AccessibleButton(
            text = "2. Oyente ➔ Sordo (Avatar AR)",
            contentDescription = "Traducir voz o texto a avatar 3D en realidad aumentada",
            onClick = onNavigateToTextToSign
        )
        Spacer(modifier = Modifier.height(16.dp))

        AccessibleButton(
            text = "3. Diccionario LSEC",
            contentDescription = "Consultar vocabulario y señas del corpus ecuatoriano",
            onClick = onNavigateToDictionary
        )
    }
}
