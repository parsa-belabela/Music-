package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
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
                        Color.White.copy(alpha = 0.16f),
                        primaryTint.copy(alpha = 0.10f),
                        Color(0xFF0C101D).copy(alpha = 0.65f)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        primaryTint.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shadowElevation = 0.dp,
                shadowColor = Color.Transparent,
                specularReflectionAlpha = 0.25f
            )

            GlassThickness.REGULAR -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2035).copy(alpha = 0.72f),
                        primaryTint.copy(alpha = 0.14f),
                        Color(0xFF0A0D18).copy(alpha = 0.84f)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.70f),
                        primaryTint.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shadowElevation = 8.dp,
                shadowColor = Color.Black.copy(alpha = 0.45f),
                specularReflectionAlpha = 0.55f
            )

            GlassThickness.THICK -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xFF181E32).copy(alpha = 0.86f),
                        primaryTint.copy(alpha = 0.18f),
                        Color(0xFF070A14).copy(alpha = 0.92f)
                    )
                ),
                borderGradient = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.75f),
                        primaryTint.copy(alpha = 0.70f),
                        Color.White.copy(alpha = 0.25f)
                    )
                ),
                shadowElevation = 16.dp,
                shadowColor = Color.Black.copy(alpha = 0.60f),
                specularReflectionAlpha = 0.65f
            )
        }
    }
}

/**
 * شیشه مایع (Liquid Glass) با انعکاس نور زنده و پویا هنگام اسکرول بالا و پایین
 * - بدون هیچ تکسچر یا خط اضافی روی دکمههای کوچک
 * - دارای پرتو انعکاس نوری ابریشمی و لبههای براق حساس به اسکرول روی کارتها
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.14f,
    borderWidth: Dp = 1.dp,
    appTheme: AppTheme? = null,
    accentColor: Color = tintColor,
    secondaryAccent: Color = Color(0xFF00E5FF)
): Modifier = composed {
    val effectiveShape = if (shape == RoundedCornerShape(20.dp) && cornerRadius != 20.dp) {
        RoundedCornerShape(cornerRadius)
    } else {
        shape
    }
    val activeTint = if (tintColor != Color.Transparent && tintColor != Color.Unspecified) {
        tintColor
    } else if (accentColor != Color.Transparent && accentColor != Color.Unspecified) {
        accentColor
    } else {
        Color(0xFF8B5CF6)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(1f) }
    }

    var windowY by remember { mutableFloatStateOf(0f) }

    // واکنش نرم و سیال انعکاس نور هنگام بالا و پایین کردن صفحه
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
        .clip(effectiveShape)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            val isSmallButton = minDim < 56.dp.toPx() || w < 72.dp.toPx()

            val outline = effectiveShape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }

            if (isSmallButton) {
                // حالت دکمههای کوچک: کریستالی، تمیز و بدون هیچ خط یا تکسچر مزاحم
                val btnFill = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        activeTint.copy(alpha = (tintAlpha * 1.1f).coerceIn(0.08f, 0.30f)),
                        Color(0xFF0B0E1A).copy(alpha = 0.70f)
                    )
                )
                val btnRim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.60f),
                        activeTint.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.20f)
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
                // حالت کارتها و پنلها: انعکاس نور زنده هنگام اسکرول + عمق سهبعدی
                val sweepProgress = (1f - smoothScrollRatio) * 1.55f - 0.25f
                val beamCenterX = w * sweepProgress
                val beamCenterY = h * (0.2f + smoothScrollRatio * 0.6f)
                val beamWidth = (w * 0.52f).coerceAtLeast(160f)

                val baseAlpha = when (thickness) {
                    GlassThickness.THIN -> 0.68f
                    GlassThickness.REGULAR -> 0.78f
                    GlassThickness.THICK -> 0.88f
                }

                val glassBodyBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF171C2E).copy(alpha = baseAlpha),
                        activeTint.copy(alpha = (tintAlpha * 1.35f).coerceIn(0.10f, 0.38f)),
                        Color(0xFF090C16).copy(alpha = (baseAlpha + 0.08f).coerceAtMost(0.95f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                // پرتو نور منشوری که با اسکرول کردن صفحه به نرمی روی شیشه حرکت میکند
                val specularReflectionBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.32f to activeTint.copy(alpha = 0.06f),
                        0.45f to Color.White.copy(alpha = 0.14f),
                        0.50f to Color.White.copy(alpha = 0.28f),
                        0.55f to secondaryAccent.copy(alpha = 0.10f),
                        0.68f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    start = Offset(beamCenterX - beamWidth, beamCenterY - beamWidth * 0.7f),
                    end = Offset(beamCenterX + beamWidth, beamCenterY + beamWidth * 0.7f)
                )

                // پخ نوری سهبعدی سقف و کف شیشه
                val topLightFactor = (1f - smoothScrollRatio * 0.5f).coerceIn(0.35f, 1f)
                val innerBevelBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.24f * topLightFactor),
                        0.12f to Color.White.copy(alpha = 0.03f),
                        0.85f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.35f)
                    )
                )

                // چرخش نقطه درخشش کادر دور کارت هماهنگ با اسکرول
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
                        0.00f to activeTint.copy(alpha = 0.80f),
                        0.35f to Color.White.copy(alpha = 0.85f),
                        0.65f to secondaryAccent.copy(alpha = 0.70f),
                        1.00f to activeTint.copy(alpha = 0.55f)
                    ),
                    start = rimStart,
                    end = rimEnd
                )

                onDrawWithContent {
                    clipPath(outlinePath) {
                        drawRect(brush = glassBodyBrush)
                        drawRect(brush = specularReflectionBrush, blendMode = BlendMode.Plus)
                        drawRect(brush = innerBevelBrush)
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
