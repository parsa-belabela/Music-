package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalAppTheme = compositionLocalOf { AppTheme.PURE_LIQUID_GLASS }

enum class GlassThickness {
    THIN,
    REGULAR,
    THICK
}

data class LiquidGlassSpec(
    val surfaceGradient: Brush,
    val borderGradient: Brush,
    val shadowElevation: Dp,
    val shadowColor: Color,
    val specularReflectionAlpha: Float = 0.0f
)

object LiquidGlassDesign {
    fun getSpec(
        thickness: GlassThickness,
        accentColor: Color = Color.White,
        theme: AppTheme = AppTheme.PURE_LIQUID_GLASS
    ): LiquidGlassSpec {
        val baseAlpha = when (thickness) {
            GlassThickness.THIN -> 0.12f
            GlassThickness.REGULAR -> 0.18f
            GlassThickness.THICK -> 0.25f
        }
        return LiquidGlassSpec(
            surfaceGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF141829).copy(alpha = 0.88f),
                    accentColor.copy(alpha = baseAlpha * 0.45f),
                    Color(0xFF070913).copy(alpha = 0.94f)
                )
            ),
            borderGradient = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                    Color(0xFF9D4EDD).copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.12f)
                )
            ),
            shadowElevation = if (thickness == GlassThickness.THICK) 8.dp else 4.dp,
            shadowColor = Color.Black.copy(alpha = 0.45f),
            specularReflectionAlpha = 0.3f
        )
    }
}

/**
 * Ultra-refined Liquid Glass Engine with natural scroll-reactive optical sheen,
 * micro-crystalline surface texture, and strictly clipped specular rim highlights (zero spill/overflow).
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.16f,
    strokeAlpha: Float = 0.38f,
    accentColor: Color = Color(0xFF9D4EDD),
    secondaryAccent: Color = Color(0xFF00E5FF),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp,
    appTheme: AppTheme? = null
): Modifier = composed {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(1f) }
    }

    val effectiveAccent = if (tintColor != Color.Transparent) tintColor else accentColor
    val effectiveGlassAlpha = if (tintColor != Color.Transparent) tintAlpha.coerceAtLeast(0.18f) else glassAlpha
    val effectiveBorderWidth = borderWidth

    var windowY by remember { mutableFloatStateOf(0f) }

    val smoothScrollRatio by animateFloatAsState(
        targetValue = (windowY / screenHeightPx).coerceIn(-0.5f, 1.5f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LiquidScrollReflection"
    )

    this
        .onGloballyPositioned { coordinates ->
            windowY = coordinates.positionInWindow().y
        }
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val cr = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            val outlinePath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f,
                        top = 0f,
                        right = w,
                        bottom = h,
                        cornerRadius = cr
                    )
                )
            }

            val sweepProgress = (1f - smoothScrollRatio) * 1.4f - 0.2f
            val beamCenterX = w * sweepProgress
            val beamCenterY = h * (0.25f + smoothScrollRatio * 0.5f)
            val beamWidth = (w * 0.6f).coerceAtLeast(160f)

            // Deep Subsurface Dark Glass Base (opaque enough to prevent see-through overlap)
            val baseGlassBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141829).copy(alpha = 0.88f),
                    effectiveAccent.copy(alpha = (effectiveGlassAlpha * 0.45f).coerceIn(0.08f, 0.35f)),
                    Color(0xFF070913).copy(alpha = 0.94f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            // Natural Specular Optical Sweep (gentle glare, no harsh streaks)
            val specularSweepBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.35f to secondaryAccent.copy(alpha = 0.03f),
                    0.48f to Color.White.copy(alpha = 0.09f),
                    0.50f to Color.White.copy(alpha = 0.16f),
                    0.52f to Color.White.copy(alpha = 0.09f),
                    0.65f to effectiveAccent.copy(alpha = 0.06f),
                    1.00f to Color.Transparent
                ),
                start = Offset(beamCenterX - beamWidth, beamCenterY - beamWidth * 0.6f),
                end = Offset(beamCenterX + beamWidth, beamCenterY + beamWidth * 0.6f)
            )

            // 3D Internal Depth Bevel
            val topLightIntensity = (1f - (smoothScrollRatio * 0.5f)).coerceIn(0.2f, 0.8f)
            val innerBevelBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.18f * topLightIntensity),
                    0.10f to Color.White.copy(alpha = 0.03f),
                    0.85f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.35f)
                )
            )

            // Dynamic Rim Border Brush
            val dynamicRimBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = strokeAlpha * 0.8f),
                    0.35f to secondaryAccent.copy(alpha = strokeAlpha * 0.7f),
                    0.65f to effectiveAccent.copy(alpha = strokeAlpha * 0.75f),
                    1.0f to Color.White.copy(alpha = strokeAlpha * 0.35f)
                ),
                start = Offset(w * (1f - smoothScrollRatio), 0f),
                end = Offset(w * smoothScrollRatio, h)
            )

            onDrawWithContent {
                // ALL GLASS LAYERS & BORDERS ARE STRICTLY CLIPPED INSIDE THE OUTLINE PATH
                // THIS GUARANTEES ZERO OVERFLOW / SPILLING PAST THE CARD BOUNDARY
                clipPath(outlinePath) {
                    // a) Deep Glass Base
                    drawRect(brush = baseGlassBrush)

                    // b) Ultra-subtle Micro-Crystalline Texture
                    val step = 8.dp.toPx().coerceAtLeast(16f)
                    var yPos = 0f
                    while (yPos < h) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.008f),
                            start = Offset(0f, yPos),
                            end = Offset(w, yPos + (step * 0.25f)),
                            strokeWidth = 1f,
                            blendMode = BlendMode.Overlay
                        )
                        yPos += step
                    }

                    // c) Natural Specular Sweep
                    drawRect(
                        brush = specularSweepBrush,
                        blendMode = BlendMode.Screen
                    )

                    // d) 3D Internal Depth Bevel
                    drawRect(brush = innerBevelBrush)

                    // e) 3D Specular Inner Rim Contour
                    drawPath(
                        path = outlinePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f * topLightIntensity),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h * 0.4f
                        ),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // f) Main Dynamic Rim Border (Drawn INSIDE clipPath to eliminate spilling)
                    drawPath(
                        path = outlinePath,
                        brush = dynamicRimBrush,
                        style = Stroke(width = (effectiveBorderWidth.toPx() * 2).coerceAtLeast(2f))
                    )
                }

                drawContent()
            }
        }
}

/**
 * Parallel glowing border for track cards with dynamic scroll optical reflection and strict boundary clipping.
 */
@Composable
fun Modifier.premiumTrackGlowingBorder(
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    isPlaying: Boolean = false,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = if (isPlaying) 1.5.dp else 1.dp
): Modifier = composed {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(1f) }
    }

    var windowY by remember { mutableFloatStateOf(0f) }

    val smoothScrollRatio by animateFloatAsState(
        targetValue = (windowY / screenHeightPx).coerceIn(-0.4f, 1.4f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TrackCardScrollReflection"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "CardRimGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.65f else 0.45f,
        targetValue = if (isPlaying) 0.85f else 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RimAlpha"
    )

    val shape = RoundedCornerShape(cornerRadius)

    this
        .onGloballyPositioned { coordinates ->
            windowY = coordinates.positionInWindow().y
        }
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val cr = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            val outlinePath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f,
                        top = 0f,
                        right = w,
                        bottom = h,
                        cornerRadius = cr
                    )
                )
            }

            val reflectionX = w * ((1f - smoothScrollRatio) * 1.4f - 0.2f)
            val reflectionWidth = (w * 0.45f).coerceAtLeast(140f)

            val deepSurfaceBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141626).copy(alpha = 0.90f),
                    primaryColor.copy(alpha = if (isPlaying) 0.20f else 0.08f),
                    Color(0xFF090A14).copy(alpha = 0.94f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val glassSweepBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.35f to secondaryColor.copy(alpha = 0.04f),
                    0.48f to Color.White.copy(alpha = 0.12f),
                    0.50f to Color.White.copy(alpha = 0.18f),
                    0.52f to primaryColor.copy(alpha = 0.08f),
                    0.65f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                start = Offset(reflectionX - reflectionWidth, -h * 0.3f),
                end = Offset(reflectionX + reflectionWidth, h * 1.3f)
            )

            val rimAngleRad = (smoothScrollRatio * PI).toFloat()
            val rimStart = Offset(
                x = (w * 0.5f) + cos(rimAngleRad) * w * 0.5f,
                y = (h * 0.5f) + sin(rimAngleRad) * h * 0.5f
            )
            val rimEnd = Offset(
                x = (w * 0.5f) - cos(rimAngleRad) * w * 0.5f,
                y = (h * 0.5f) - sin(rimAngleRad) * h * 0.5f
            )

            val rimBorderBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to primaryColor.copy(alpha = glowAlpha),
                    0.35f to secondaryColor.copy(alpha = glowAlpha * 0.9f),
                    0.50f to Color.White.copy(alpha = 0.85f),
                    0.65f to primaryColor.copy(alpha = glowAlpha * 0.85f),
                    1.00f to primaryColor.copy(alpha = glowAlpha)
                ),
                start = rimStart,
                end = rimEnd
            )

            onDrawWithContent {
                clipPath(outlinePath) {
                    // 1. Deep Surface
                    drawRect(brush = deepSurfaceBrush)

                    // 2. Micro-crystalline grain
                    val grainStep = 6.dp.toPx().coerceAtLeast(12f)
                    var gy = 0f
                    while (gy < h) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.008f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f,
                            blendMode = BlendMode.Overlay
                        )
                        gy += grainStep
                    }

                    // 3. Smooth Glass Optical Sweep
                    drawRect(
                        brush = glassSweepBrush,
                        blendMode = BlendMode.Screen
                    )

                    // 4. 3D Bevel
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    )

                    // 5. Main Rim Border (Clipped strictly inside to prevent edge spilling)
                    drawPath(
                        path = outlinePath,
                        brush = rimBorderBrush,
                        style = Stroke(width = (borderWidth.toPx() * 2).coerceAtLeast(2f))
                    )
                }

                drawContent()
            }
        }
}

fun Modifier.neonParallelRim(
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.5.dp
): Modifier = composed {
    this.premiumTrackGlowingBorder(
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isPlaying = false,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth
    )
}
