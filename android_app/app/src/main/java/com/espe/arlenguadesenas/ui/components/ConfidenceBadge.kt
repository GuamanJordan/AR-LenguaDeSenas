package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espe.arlenguadesenas.ui.theme.ConfidenceHigh
import com.espe.arlenguadesenas.ui.theme.ConfidenceLow
import com.espe.arlenguadesenas.ui.theme.ConfidenceMedium

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    val (bgColor, label) = when {
        confidence >= 0.80f -> Pair(ConfidenceHigh, "Certeza Alta: ${(confidence * 100).toInt()}%")
        confidence >= 0.60f -> Pair(ConfidenceMedium, "Certeza Media: ${(confidence * 100).toInt()}%")
        else -> Pair(ConfidenceLow, "Ajuste el encuadre")
    }

    Row(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}
