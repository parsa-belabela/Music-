package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
import kotlin.math.min
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
        val primaryTint = if (accentColor != Color.Transparent && accentColor != Color.Unspecified) {
            accentColor
        } else {
            Color(0xFF00E5FF)
        }

        return when (thickness) {
            GlassThickness.THIN -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xCC121626),
                        Color(0xD90A0D18)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        primaryTint.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.18f)
                    )
                ),
                shadowElevation = 0.dp,
                shadowColor = Color.Transparent,
                specularReflectionAlpha = 0.25f
            )

            GlassThickness.REGULAR -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xD9161B2E),
                        Color(0xE60B0E1A)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.70f),
                        primaryTint.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.22f)
                    )
                ),
                shadowElevation = 8.dp,
                shadowColor = Color.Black.copy(alpha = 0.50f),
                specularReflectionAlpha = 0.50f
            )

            GlassThickness.THICK -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xEB151A2D),
                        Color(0xF5080B14)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.78f),
                        primaryTint.copy(alpha = 0.68f),
                        Color.White.copy(alpha = 0.25f)
                    )
                ),
                shadowElevation = 16.dp,
                shadowColor = Color.Black.copy(alpha = 0.65f),
                specularReflectionAlpha = 0.60f
            )
        }
    }
}

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.14f,
    borderWidth: Dp = 1.dp,
    appTheme: AppTheme? = null,
    accentColor: Color = tintColor,
    secondaryAccent: Color = Color(0xFF00E5FF)
): Modifier = composed {
    val activeTint = if (tintColor != Color.Transparent && tintColor != Color.Unspecified) {
        tintColor
    } else if (accentColor != Color.Transparent && accentColor != Color.Unspecified) {
        accentColor
    } else {
        Color(0xFF8B5CF6)
    }

    // محدودسازی درخشندگی رنگ پس‌زمینه کارت‌ها تا متن‌ها و آیکن‌های سفید همیشه ۱۰۰٪ خوانا و واضح باشند
    val safeTint = remember(activeTint) {
        val lum = 0.299f * activeTint.red + 0.587f * activeTint.green + 0.114f * activeTint.blue
        if (lum > 0.65f) {
            Color(
                red = activeTint.red * 0.55f,
                green = activeTint.green * 0.55f,
                blue = activeTint.blue * 0.55f,
                alpha = 1f
            )
        } else {
            activeTint
        }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(1f) }
    }

    var windowY by remember { mutableFloatStateOf(0f) }

    val smoothScrollRatio by animateFloatAsState(
        targetValue = (windowY / screenHeightPx).coerceIn(-0.3f, 1.3f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ScrollLightReflection"
    )

    this
        .onGloballyPositioned { coordinates ->
            windowY = coordinates.positionInWindow().y
        }
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            val isSmallButton = minDim < 56.dp.toPx() || w < 72.dp.toPx()

            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }

            if (isSmallButton) {
                val btnFill = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD8161B2E),
                        safeTint.copy(alpha = 0.28f),
                        Color(0xEE090C16)
                    )
                )
                val btnRim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        activeTint.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.25f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                onDrawWithContent {
                    drawPath(path = outlinePath, brush = btnFill)
                    drawContent()
                    drawPath(
                        path = outlinePath,
                        brush = btnRim,
                        style = Stroke(width = borderWidth.toPx())
                    )
                }
            } else {
                val sweepProgress = (1f - smoothScrollRatio) * 1.55f - 0.25f
                val beamCenterX = w * sweepProgress
                val beamCenterY = h * (0.2f + smoothScrollRatio * 0.6f)
                val beamWidth = (w * 0.52f).coerceAtLeast(160f)

                val baseAlpha = when (thickness) {
                    GlassThickness.THIN -> 0.80f
                    GlassThickness.REGULAR -> 0.86f
                    GlassThickness.THICK -> 0.92f
                }

                val glassBodyBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF121626).copy(alpha = baseAlpha),
                        safeTint.copy(alpha = (tintAlpha * 1.15f).coerceIn(0.12f, 0.32f)),
                        Color(0xFF080A14).copy(alpha = (baseAlpha + 0.05f).coerceAtMost(0.96f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                val specularReflectionBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.35f to safeTint.copy(alpha = 0.05f),
                        0.46f to Color.White.copy(alpha = 0.10f),
                        0.50f to Color.White.copy(alpha = 0.18f),
                        0.54f to secondaryAccent.copy(alpha = 0.08f),
                        0.65f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    start = Offset(beamCenterX - beamWidth, beamCenterY - beamWidth * 0.7f),
                    end = Offset(beamCenterX + beamWidth, beamCenterY + beamWidth * 0.7f)
                )

                val rimAngle = (smoothScrollRatio * PI).toFloat()
                val rimStart = Offset(
                    x = (w * 0.5f) + cos(rimAngle) * w * 0.6f,
                    y = (h * 0.5f) + sin(rimAngle) * h * 0.6f
                )
                val rimEnd = Offset(
                    x = (w * 0.5f) - cos(rimAngle) * w * 0.6f,
                    y = (h * 0.5f) - sin(rimAngle) * h * 0.6f
                )

                val dynamicBorderBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to activeTint.copy(alpha = 0.85f),
                        0.35f to Color.White.copy(alpha = 0.85f),
                        0.65f to secondaryAccent.copy(alpha = 0.75f),
                        1.00f to activeTint.copy(alpha = 0.60f)
                    ),
                    start = rimStart,
                    end = rimEnd
                )

                onDrawWithContent {
                    clipPath(outlinePath) {
                        drawRect(brush = glassBodyBrush)
                        drawRect(brush = specularReflectionBrush)
                    }
                    drawContent()
                    drawPath(
                        path = outlinePath,
                        brush = dynamicBorderBrush,
                        style = Stroke(width = borderWidth.toPx())
                    )
                }
            }
        }
}

@Composable
fun Modifier.premiumTrackGlowingBorder(
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    isPlaying: Boolean = false,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = if (isPlaying) 1.8.dp else 1.25.dp
): Modifier = composed {
    this.liquidGlass(
        shape = RoundedCornerShape(cornerRadius),
        thickness = if (isPlaying) GlassThickness.THICK else GlassThickness.REGULAR,
        tintColor = primaryColor,
        tintAlpha = if (isPlaying) 0.24f else 0.14f,
        borderWidth = borderWidth,
        secondaryAccent = secondaryColor
    )
}
