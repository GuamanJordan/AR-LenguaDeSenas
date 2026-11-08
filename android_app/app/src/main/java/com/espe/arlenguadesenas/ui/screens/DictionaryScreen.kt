package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DictionaryItem(val term: String, val gloss: String, val category: String)

/**
 * Diccionario oficial de la Lengua de Señas Ecuatoriana (LSEC)
 * basado en los estándares de CONADIS (Gabriel Román) y la pedagogía
 * inclusiva de GioSEÑAS (Saludos y cortesía).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    val items = remember {
        listOf(
            DictionaryItem("Hola / Saludo", "HOLA", "Saludos (GioSEÑAS)"),
            DictionaryItem("Buenos días", "BUENOS DÍAS", "Saludos (GioSEÑAS)"),
            DictionaryItem("Buenas tardes", "BUENAS TARDES", "Saludos (GioSEÑAS)"),
            DictionaryItem("Buenas noches", "BUENAS NOCHES", "Saludos (GioSEÑAS)"),
            DictionaryItem("Chao / Adiós", "CHAO (ADIÓS)", "Saludos (GioSEÑAS)"),
            DictionaryItem("¿Cómo estás?", "¿CÓMO ESTÁS?", "Preguntas (GioSEÑAS)"),
            DictionaryItem("Mucho gusto", "MUCHO GUSTO", "Cortesía (GioSEÑAS)"),
            DictionaryItem("Gracias", "GRACIAS", "Cortesía (CONADIS)"),
            DictionaryItem("Por favor", "POR FAVOR", "Cortesía (CONADIS)"),
            DictionaryItem("De nada", "DE NADA", "Cortesía (CONADIS)"),
            DictionaryItem("Ayuda / Auxilio", "AYUDA", "Emergencia (CONADIS)"),
            DictionaryItem("Sí (Afirmación)", "SÍ (AFIRMACIÓN)", "Respuestas (CONADIS)"),
            DictionaryItem("No (Negación)", "NO (NEGACIÓN)", "Respuestas (CONADIS)"),
            DictionaryItem("Te quiero / Amor", "TE QUIERO", "Afecto"),
            DictionaryItem("Baño", "BANO", "Necesidades"),
            DictionaryItem("Profesor / Docente", "PROFESOR", "Universidad"),
            DictionaryItem("Clase académica", "CLASE", "Universidad"),
            DictionaryItem("Entiendo", "ENTENDER", "Universidad"),
            DictionaryItem("No entiendo", "ENTENDER NO", "Universidad")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diccionario LSEC (CONADIS / GioSEÑAS)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al menú principal"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = item.term, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Glosa LSEC: ${item.gloss}", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Categoría: ${item.category}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
