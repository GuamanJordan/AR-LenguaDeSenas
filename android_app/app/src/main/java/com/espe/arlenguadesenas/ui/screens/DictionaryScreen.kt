package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DictionaryItem(val term: String, val gloss: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    val items = remember {
        listOf(
            DictionaryItem("Hola", "HOLA", "Saludos"),
            DictionaryItem("Buenos días", "BUENO DIA", "Saludos"),
            DictionaryItem("Gracias", "GRACIAS", "Cortesía"),
            DictionaryItem("Por favor", "POR-FAVOR", "Cortesía"),
            DictionaryItem("Ayuda", "AYUDAR", "Emergencia"),
            DictionaryItem("Baño", "BANO", "Necesidades"),
            DictionaryItem("Profesor", "PROFESOR", "Universidad"),
            DictionaryItem("Clase", "CLASE", "Universidad"),
            DictionaryItem("Entiendo", "ENTENDER", "Universidad"),
            DictionaryItem("No entiendo", "ENTENDER NO", "Universidad")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diccionario LSEC (CONADIS)") },
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
                        Text(text = "Glosa LSEC: ${item.gloss}", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Categoría: ${item.category}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
