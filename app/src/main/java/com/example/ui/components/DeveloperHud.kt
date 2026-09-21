package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioAnalysisData
import com.example.data.model.PlaybackState

@Composable
fun DeveloperHud(
    analysisData: AudioAnalysisData,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC05050A))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "⚡ AURA DSP REAL-TIME TELEMETRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8)
                )
            )
            Text(
                text = "FPS: ${analysisData.currentFps} | Target: 60/120Hz",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.White
            )
            Text(
                text = "RMS: ${"%.3f".format(analysisData.rms)} | Peak: ${"%.3f".format(analysisData.peak)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.White
            )
            Text(
                text = "Bass: ${"%.3f".format(analysisData.bass)} | Kick: ${if (analysisData.isKick) "TRIGGERED" else "WAIT"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (analysisData.isKick) Color(0xFFF43F5E) else Color.White
            )
            Text(
                text = "Pulse: ${"%.2f".format(analysisData.kickPulse)} | Halo: ${"%.2f".format(analysisData.haloExpansion)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFFA855F7)
            )
            Text(
                text = "Track Pos: ${playbackState.positionFormatted} (${playbackState.currentPositionMs}ms)",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFFA0A0B0)
            )
        }
    }
}
