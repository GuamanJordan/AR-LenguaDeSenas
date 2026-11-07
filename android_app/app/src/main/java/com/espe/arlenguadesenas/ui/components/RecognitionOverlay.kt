package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecognitionOverlay(isHandDetected: Boolean, modifier: Modifier = Modifier) {
    val borderColor = if (isHandDetected) Color(0xFF00E676) else Color(0xFFFF5252)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp, 360.dp)
                .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
                .padding(16.dp)
        )
    }
}
