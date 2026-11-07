package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AvatarControlsBar(
    isPlaying: Boolean,
    currentSpeed: Float,
    onTogglePlay: () -> Unit,
    onRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onTogglePlay) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar seña" else "Reproducir seña"
            )
        }
        IconButton(onClick = onRepeat) {
            Icon(Icons.Default.Replay, contentDescription = "Repetir seña")
        }
        Button(onClick = {
            val newSpeed = if (currentSpeed == 1.0f) 0.5f else 1.0f
            onSpeedChange(newSpeed)
        }) {
            Text(text = if (currentSpeed == 0.5f) "0.5x Lento" else "1.0x Normal")
        }
    }
}
