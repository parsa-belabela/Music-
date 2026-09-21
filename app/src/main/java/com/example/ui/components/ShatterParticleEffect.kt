package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ParticleShard(
    val initialX: Float,
    val initialY: Float,
    val targetOffsetX: Float,
    val targetOffsetY: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val isSquare: Boolean
)

@Composable
fun DisintegrationOverlay(
    isDisintegrating: Boolean,
    primaryColor: Color,
    accentColor: Color,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isDisintegrating) return

    val progress = remember { Animatable(0f) }

    // Generate random particle shards
    val shards = remember {
        val colors = listOf(
            primaryColor,
            accentColor,
            Color(0xFFEF4444),
            Color(0xFFF59E0B),
            Color(0xFF8B5CF6),
            Color.White
        )
        List(45) {
            val random = Random(it * 31)
            ParticleShard(
                initialX = random.nextFloat(),
                initialY = random.nextFloat(),
                targetOffsetX = (random.nextFloat() - 0.5f) * 360f,
                targetOffsetY = -random.nextFloat() * 280f - 40f,
                size = random.nextFloat() * 10f + 4f,
                color = colors[random.nextInt(colors.size)],
                rotation = random.nextFloat() * 720f - 360f,
                isSquare = random.nextBoolean()
            )
        }
    }

    LaunchedEffect(isDisintegrating) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentProgress = progress.value
            val alpha = (1f - currentProgress).coerceIn(0f, 1f)

            shards.forEach { shard ->
                val currentX = shard.initialX * size.width + shard.targetOffsetX * currentProgress
                val currentY = shard.initialY * size.height + shard.targetOffsetY * currentProgress
                val currentScale = (1f - currentProgress * 0.7f).coerceIn(0f, 1f)
                val shardColor = shard.color.copy(alpha = alpha * 0.9f)

                rotate(
                    degrees = shard.rotation * currentProgress,
                    pivot = Offset(currentX, currentY)
                ) {
                    val pSize = shard.size * currentScale
                    if (shard.isSquare) {
                        drawRect(
                            color = shardColor,
                            topLeft = Offset(currentX - pSize / 2, currentY - pSize / 2),
                            size = Size(pSize, pSize)
                        )
                    } else {
                        drawCircle(
                            color = shardColor,
                            radius = pSize / 2,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }
        }
    }
}
