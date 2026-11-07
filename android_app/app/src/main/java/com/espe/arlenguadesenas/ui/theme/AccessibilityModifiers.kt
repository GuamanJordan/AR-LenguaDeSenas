package com.espe.arlenguadesenas.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Target táctil mínimo de 48dp conforme norma WCAG 2.5.8
fun Modifier.accessibleTouchTarget(description: String, onClick: () -> Unit): Modifier = this
    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    .semantics { contentDescription = description }
    .clickable(onClick = onClick)
