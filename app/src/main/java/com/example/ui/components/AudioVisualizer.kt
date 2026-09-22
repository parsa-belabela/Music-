package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.AppTheme
import com.example.data.model.VisualizerMode
import com.example.ui.theme.LocalAppTheme
import kotlin.math.*
import kotlin.random.Random

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float
)

@Composable
fun AudioVisualizer(
    mode: VisualizerMode,
    palette: AmbientPalette,
    modifier: Modifier = Modifier,
    sensitivity: Float = 1.0f,
    glow: Float = 0.85f,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() }
) {
    val currentTheme = LocalAppTheme.current

    // Generate persistent particles for Particle Field mode
    val particles = remember {
        List(48) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.005f,
                vy = (Random.nextFloat() - 0.5f) * 0.005f,
                size = Random.nextFloat() * 6f + 2f,
                alpha = Random.nextFloat() * 0.6f + 0.2f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val analysisData = analysisDataProvider()
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        when (mode) {
            VisualizerMode.AMBIENT_HALO, VisualizerMode.BASS_GLOW -> {
                // Soft radial breathing aura
                val expansion = (analysisData.haloExpansion * 0.4f + analysisData.kickPulse * 0.3f) * sensitivity
                val radius = (min(w, h) * 0.35f) * (1f + expansion)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = (0.5f * glow).coerceIn(0f, 1f)),
                            palette.secondary.copy(alpha = (0.25f * glow).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            VisualizerMode.SPECTRUM -> {
                // Vertical responsive equalizer bars
                val bands = analysisData.fftBands
                val barCount = 24
                val barWidth = (w / (barCount * 1.6f)).coerceAtLeast(4f)
                val spacing = barWidth * 0.6f
                val totalWidth = barCount * (barWidth + spacing) - spacing
                val startX = (w - totalWidth) / 2f

                for (i in 0 until barCount) {
                    val bandVal = (bands.getOrElse(i) { 0f } * sensitivity).coerceIn(0.04f, 1f)
                    val barHeight = (h * 0.45f * bandVal).coerceAtLeast(4f)
                    val x = startX + i * (barWidth + spacing)
                    val y = h / 2f - barHeight / 2f

                    val barColor = when {
                        i < 6 -> palette.primary
                        i < 16 -> palette.secondary
                        else -> palette.accent
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = 0.95f), barColor.copy(alpha = 0.25f)),
                            startY = y,
                            endY = y + barHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            VisualizerMode.CIRCULAR_SPECTRUM -> {
                // Circular radially expanding bars
                val baseRadius = min(w, h) * 0.28f * (1f + analysisData.kickPulse * 0.15f)
                val barCount = 36
                val angleStep = (2 * PI / barCount).toFloat()

                for (i in 0 until barCount) {
                    val bandVal = (analysisData.fftBands.getOrElse(i % 32) { 0f } * sensitivity).coerceIn(0.05f, 1f)
                    val barLen = (min(w, h) * 0.2f * bandVal).coerceAtLeast(6f)
                    val angle = i * angleStep

                    val start = Offset(
                        center.x + baseRadius * cos(angle),
                        center.y + baseRadius * sin(angle)
                    )
                    val end = Offset(
                        center.x + (baseRadius + barLen) * cos(angle),
                        center.y + (baseRadius + barLen) * sin(angle)
                    )

                    drawLine(
                        color = if (i % 2 == 0) palette.primary else palette.secondary,
                        start = start,
                        end = end,
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerMode.WAVEFORM -> {
                // Oscilloscope fluid smooth waveform
                val wave = analysisData.waveform
                val path = Path()
                val stepX = w / (wave.size - 1)

                for (i in wave.indices) {
                    val x = i * stepX
                    val y = h / 2f + (wave[i] * (h * 0.25f) * sensitivity)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = palette.secondary.copy(alpha = 0.85f * glow),
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            VisualizerMode.RADIAL_WAVE -> {
                // Circular undulating wave
                val baseRadius = min(w, h) * 0.3f
                val path = Path()
                val points = 48
                for (i in 0..points) {
                    val angle = (i * 2 * PI / points).toFloat()
                    val waveIdx = (i % analysisData.waveform.size)
                    val displacement = analysisData.waveform[waveIdx] * 35f * sensitivity
                    val r = baseRadius + displacement + analysisData.kickPulse * 20f
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(palette.primary.copy(alpha = 0.7f), palette.accent.copy(alpha = 0.3f)),
                        center = center
                    ),
                    style = Stroke(width = 3.5f)
                )
            }

            VisualizerMode.PULSE_RING -> {
                // Concentric expanding acoustic shockwaves
                val ringCount = 4
                val kick = analysisData.kickPulse
                for (i in 1..ringCount) {
                    val progress = ((i * 0.25f + kick * 0.35f) % 1f)
                    val ringRadius = (min(w, h) * 0.45f) * progress
                    val alpha = (1f - progress) * 0.7f * glow
                    drawCircle(
                        color = if (i % 2 == 0) palette.primary.copy(alpha = alpha) else palette.secondary.copy(alpha = alpha),
                        radius = ringRadius.coerceAtLeast(1f),
                        center = center,
                        style = Stroke(width = 2.5f + (1f - progress) * 3f)
                    )
                }
            }

            VisualizerMode.AURORA -> {
                // Atmospheric northern lights curtains
                val bands = analysisData.fftBands
                val path = Path()
                path.moveTo(0f, h)
                val segments = 24
                for (i in 0..segments) {
                    val x = i * (w / segments)
                    val band = bands.getOrElse(i) { 0.2f } * sensitivity
                    val y = (h * 0.5f) - (band * h * 0.3f) + sin(i * 0.6f + analysisData.totalEnergy * 2f) * 20f
                    path.lineTo(x, y)
                }
                path.lineTo(w, h)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = 0.45f * glow),
                            palette.primary.copy(alpha = 0.2f * glow),
                            Color.Transparent
                        )
                    )
                )
            }

            VisualizerMode.LIQUID -> {
                // Organic viscous blob
                val baseRadius = min(w, h) * 0.26f * (1f + analysisData.bass * 0.3f)
                val path = Path()
                val points = 16
                for (i in 0..points) {
                    val angle = (i * 2 * PI / points).toFloat()
                    val wobble = sin(angle * 3f + analysisData.totalEnergy * 4f) * (20f * sensitivity)
                    val r = baseRadius + wobble + analysisData.kickPulse * 18f
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = 0.6f),
                            palette.secondary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center
                    )
                )
            }

            VisualizerMode.PARTICLE_FIELD -> {
                // Floating reactive particles
                val kick = analysisData.kickPulse
                for (p in particles) {
                    p.x = (p.x + p.vx * (1f + kick * 2f) + 1f) % 1f
                    p.y = (p.y + p.vy * (1f + kick * 2f) + 1f) % 1f

                    val px = p.x * w
                    val py = p.y * h
                    val pRadius = p.size * (1f + analysisData.bass * 0.8f + kick * 1.2f)

                    drawCircle(
                        color = palette.secondary.copy(alpha = (p.alpha * (1f + kick)).coerceIn(0f, 1f)),
                        radius = pRadius,
                        center = Offset(px, py)
                    )
                }
            }

            VisualizerMode.DOTS -> {
                // Grid of reactive dots
                val cols = 8
                val rows = 5
                val cellW = w / cols
                val cellH = (h * 0.5f) / rows
                val startY = h * 0.25f

                for (c in 0 until cols) {
                    for (r in 0 until rows) {
                        val idx = (c * rows + r) % analysisData.fftBands.size
                        val band = (analysisData.fftBands[idx] * sensitivity).coerceIn(0.1f, 1f)
                        val radius = (min(cellW, cellH) * 0.35f * band).coerceAtLeast(3f)
                        val x = c * cellW + cellW / 2f
                        val y = startY + r * cellH + cellH / 2f

                        drawCircle(
                            color = if (r % 2 == 0) palette.primary.copy(alpha = band) else palette.accent.copy(alpha = band),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            VisualizerMode.CINEMATIC_FOG -> {
                // Layered undulating mist clouds
                val energy = analysisData.totalEnergy
                val radius = min(w, h) * 0.48f * (1f + energy * 0.25f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.haloGlow.copy(alpha = (0.55f * glow).coerceIn(0f, 1f)),
                            palette.primary.copy(alpha = (0.25f * glow).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(center.x - 20f, center.y - 15f),
                        radius = radius * 1.2f
                    ),
                    radius = radius * 1.2f,
                    center = center
                )
            }
        }
    }
}
