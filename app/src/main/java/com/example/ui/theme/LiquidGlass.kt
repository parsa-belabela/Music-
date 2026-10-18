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
 * موتور شیشه‌ای هوشمند (Size-Adaptive Liquid Glass):
 * - روی دکمه‌های کوچک: شیشه کریستالی بسیار تمیز، بدون خطوط اضافه و منطبق با فرم دایره/کپسول
 * - روی کارت‌ها و پنل‌های بزرگ: انعکاس نور منشوری حساس به اسکرول + عمق سه‌بعدی
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.16f,
    strokeAlpha: Float = 0.45f,
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
    val effectiveGlassAlpha = if (tintColor != Color.Transparent) tintAlpha.coerceAtLeast(0.12f) else glassAlpha

    var windowY by remember { mutableFloatStateOf(0f) }

    val smoothScrollRatio by animateFloatAsState(
        targetValue = (windowY / screenHeightPx).coerceIn(-0.4f, 1.4f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AdaptiveLiquidScroll"
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

            // تشخیص خودکار دکمه‌های کوچک (کمتر از 64dp) برای جلوگیری از شلوغی و به‌هم‌ریختگی
            val isSmallButton = minDim < 64.dp.toPx()

            // استخراج دقیق فرم واقعی کامپوننت (چه دایره باشد، چه کپسول، چه مستطیل گرد)
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }

            if (isSmallButton) {
                // =========================================================================
                // حالت ویژه دکمه‌های کوچک: ظریف، براق، بدون خطوط اضافه و کاملاً تمیز
                // =========================================================================
                val buttonGlassBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (effectiveGlassAlpha * 1.35f).coerceAtMost(0.32f)),
                        effectiveAccent.copy(alpha = (effectiveGlassAlpha * 0.45f).coerceAtMost(0.18f)),
                        Color(0xFF10101C).copy(alpha = 0.65f)
                    )
                )

                val buttonTopLensBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.35f, h * 0.25f),
                    radius = (minDim * 0.75f).coerceAtLeast(1f)
                )

                val buttonBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (strokeAlpha * 1.1f).coerceAtMost(0.65f)),
                        secondaryAccent.copy(alpha = strokeAlpha * 0.7f),
                        effectiveAccent.copy(alpha = strokeAlpha * 0.75f),
                        Color.White.copy(alpha = strokeAlpha * 0.25f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                onDrawWithContent {
                    drawPath(path = outlinePath, brush = buttonGlassBrush)
                    drawPath(path = outlinePath, brush = buttonTopLensBrush, blendMode = BlendMode.Plus)
                    drawContent()
                    drawPath(
                        path = outlinePath,
                        brush = buttonBorderBrush,
                        style = Stroke(width = 0.95.dp.toPx())
                    )
                }
            } else {
                // =========================================================================
                // حالت ویژه کارت‌ها و پنل‌های بزرگ: انعکاس نور اسکرول + عمق سه‌بعدی
                // =========================================================================
                val sweepProgress = (1f - smoothScrollRatio) * 1.5f - 0.25f
                val beamCenterX = w * sweepProgress
                val beamCenterY = h * (0.25f + smoothScrollRatio * 0.5f)
                val beamWidth = (w * 0.45f).coerceAtLeast(140f)

                val baseGlassBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.76f),
                        effectiveAccent.copy(alpha = effectiveGlassAlpha * 0.55f),
                        Color(0xFF0B0B14).copy(alpha = 0.85f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                val specularSweepBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.34f to secondaryAccent.copy(alpha = 0.04f),
                        0.45f to Color.White.copy(alpha = 0.12f),
                        0.50f to Color.White.copy(alpha = 0.26f),
                        0.55f to effectiveAccent.copy(alpha = 0.12f),
                        0.66f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    start = Offset(beamCenterX - beamWidth, beamCenterY - beamWidth * 0.65f),
                    end = Offset(beamCenterX + beamWidth, beamCenterY + beamWidth * 0.65f)
                )

                val topLightIntensity = (1f - (smoothScrollRatio * 0.5f)).coerceIn(0.3f, 0.95f)
                val innerBevelBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.24f * topLightIntensity),
                        0.10f to Color.White.copy(alpha = 0.04f),
                        0.85f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.38f)
                    )
                )

                val dynamicRimBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = strokeAlpha * 0.90f),
                        0.35f to secondaryAccent.copy(alpha = strokeAlpha * 0.85f),
                        0.65f to effectiveAccent.copy(alpha = strokeAlpha * 0.90f),
                        1.0f to Color.White.copy(alpha = strokeAlpha * 0.35f)
                    ),
                    start = Offset(w * (1f - smoothScrollRatio), 0f),
                    end = Offset(w * smoothScrollRatio, h)
                )

                onDrawWithContent {
                    clipPath(outlinePath) {
                        drawRect(brush = baseGlassBrush)
                        drawRect(brush = specularSweepBrush, blendMode = BlendMode.Plus)
                        drawRect(brush = innerBevelBrush)
                    }
                    drawContent()
                    drawPath(
                        path = outlinePath,
                        brush = dynamicRimBrush,
                        style = Stroke(width = 1.25.dp.toPx())
                    )
                }
            }
        }
}

/**
 * کادر مستطیلی موازی با خط رنگی درخشان و انعکاس نور اسکرول مخصوص کارت‌های آهنگ و پنل پلیر
 */
@Composable
fun Modifier.premiumTrackGlowingBorder(
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    isPlaying: Boolean = false,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = if (isPlaying) 1.8.dp else 1.25.dp
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
        initialValue = if (isPlaying) 0.72f else 0.52f,
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RimAlpha"
    )

    val shape = RoundedCornerShape(cornerRadius)

    this
        .onGloballyPositioned { coordinates ->
            windowY = coordinates.positionInWindow().y
        }
        .drawWithCache {
            val w = size.width
            val h = size.height
            val cr = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            val clipRoundPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, w, h, cr))
            }

            val reflectionX = w * ((1f - smoothScrollRatio) * 1.4f - 0.2f)
            val reflectionWidth = (w * 0.45f).coerceAtLeast(140f)

            val outerGlowBrush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha * 0.35f),
                    secondaryColor.copy(alpha = glowAlpha * 0.28f),
                    primaryColor.copy(alpha = glowAlpha * 0.35f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val deepSurfaceBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF151524).copy(alpha = 0.78f),
                    primaryColor.copy(alpha = if (isPlaying) 0.22f else 0.10f),
                    Color(0xFF0C0C16).copy(alpha = 0.86f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val glassSweepBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.36f to secondaryColor.copy(alpha = 0.04f),
                    0.46f to Color.White.copy(alpha = 0.13f),
                    0.50f to Color.White.copy(alpha = 0.24f),
                    0.54f to primaryColor.copy(alpha = 0.12f),
                    0.64f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                start = Offset(reflectionX - reflectionWidth, -h * 0.3f),
                end = Offset(reflectionX + reflectionWidth, h * 1.3f)
            )

            val rimAngleRad = (smoothScrollRatio * PI).toFloat()
            val rimStart = Offset(
                x = (w * 0.5f) + cos(rimAngleRad) * w * 0.6f,
                y = (h * 0.5f) + sin(rimAngleRad) * h * 0.6f
            )
            val rimEnd = Offset(
                x = (w * 0.5f) - cos(rimAngleRad) * w * 0.6f,
                y = (h * 0.5f) - sin(rimAngleRad) * h * 0.6f
            )

            val rimBorderBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to primaryColor.copy(alpha = glowAlpha),
                    0.35f to secondaryColor.copy(alpha = glowAlpha * 0.92f),
                    0.50f to Color.White.copy(alpha = 0.85f),
                    0.65f to secondaryColor.copy(alpha = glowAlpha * 0.88f),
                    1.00f to primaryColor.copy(alpha = glowAlpha)
                ),
                start = rimStart,
                end = rimEnd
            )

            onDrawWithContent {
                drawRoundRect(
                    brush = outerGlowBrush,
                    cornerRadius = cr,
                    style = Stroke(width = (borderWidth + 2.5.dp).toPx())
                )

                clipPath(clipRoundPath) {
                    drawRect(brush = deepSurfaceBrush)
                    drawRect(brush = glassSweepBrush, blendMode = BlendMode.Plus)
                }

                drawContent()

                drawRoundRect(
                    brush = rimBorderBrush,
                    cornerRadius = cr,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
        }
        .clip(shape)
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
