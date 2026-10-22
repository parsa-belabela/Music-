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
        targetValue = (windowY / screenHeightPx).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
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
            val strokePx = borderWidth.toPx().coerceAtLeast(1f)
            val halfStroke = strokePx / 2f

            val isSmallButton = minDim < 60.dp.toPx()

            // فرم دقیق و اینست‌شده برای حاشیه جهت جلوگیری از بیرون‌زدگی یا کات‌شدن
            val crPx = cornerRadius.toPx().coerceAtLeast(0f)
            val insetCr = CornerRadius((crPx - halfStroke).coerceAtLeast(0f), (crPx - halfStroke).coerceAtLeast(0f))
            val insetRect = RoundRect(
                left = halfStroke,
                top = halfStroke,
                right = (w - halfStroke).coerceAtLeast(halfStroke),
                bottom = (h - halfStroke).coerceAtLeast(halfStroke),
                cornerRadius = insetCr
            )
            val insetPath = Path().apply { addRoundRect(insetRect) }

            val outline = shape.createOutline(size, layoutDirection, this)
            val fillPath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }

            if (isSmallButton) {
                // =========================================================================
                // دکمه‌های کوچک: کاملاً شفاف، سبک، کریستالی با رفلکس نرم بالای دکمه
                // =========================================================================
                val buttonGlassBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (effectiveGlassAlpha * 1.1f).coerceIn(0.12f, 0.25f)),
                        effectiveAccent.copy(alpha = (effectiveGlassAlpha * 0.40f).coerceAtMost(0.14f)),
                        Color(0xFF0F111E).copy(alpha = 0.72f)
                    )
                )

                val buttonBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        effectiveAccent.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.12f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                onDrawWithContent {
                    drawPath(path = fillPath, brush = buttonGlassBrush)
                    drawContent()
                    drawPath(
                        path = insetPath,
                        brush = buttonBorderBrush,
                        style = Stroke(width = strokePx)
                    )
                }
            } else {
                // =========================================================================
                // کارت‌ها و پنل‌های بزرگ: چندلایه، بافت شیشه کریستالی مات و رفلکس لبه‌ای
                // =========================================================================
                val baseGlassBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF1E2238).copy(alpha = 0.88f),
                        0.18f to Color(0xFF141728).copy(alpha = 0.84f),
                        0.50f to effectiveAccent.copy(alpha = (effectiveGlassAlpha * 0.35f).coerceAtMost(0.16f)),
                        0.78f to Color(0xFF0C0E1A).copy(alpha = 0.92f),
                        1.00f to Color(0xFF06070E).copy(alpha = 0.96f)
                    )
                )

                // انعکاس نرم و طبیعی افقی منطبق بر اسکرول
                val shimmerX = w * (0.15f + smoothScrollRatio * 0.70f)
                val shimmerRadius = (w * 0.65f).coerceAtLeast(180f)
                val naturalGleamBrush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.12f),
                        0.25f to secondaryAccent.copy(alpha = 0.08f),
                        0.55f to effectiveAccent.copy(alpha = 0.05f),
                        0.85f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    center = Offset(shimmerX, h * 0.20f),
                    radius = shimmerRadius
                )

                // پخ نوری لبه بالا برای ایجاد عمق سه‌بعدی شیشه (Specular Top Bevel Light)
                val topBevelBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.22f),
                        0.06f to Color.White.copy(alpha = 0.08f),
                        0.35f to Color.Transparent,
                        0.85f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.35f)
                    )
                )

                // تکسچر ظریف شیشه مات کریستالی (Frosted Micro-Grain & Specular Edge Refraction)
                val frostedGrainSheen = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.06f),
                        0.30f to Color.Transparent,
                        0.60f to secondaryAccent.copy(alpha = 0.04f),
                        0.80f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = 0.03f)
                    ),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                )

                // حاشیه کریستالی منشوری اینست‌شده
                val crystalRimBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.55f),
                        0.25f to secondaryAccent.copy(alpha = 0.35f),
                        0.50f to Color.White.copy(alpha = 0.20f),
                        0.75f to effectiveAccent.copy(alpha = 0.38f),
                        1.00f to Color.White.copy(alpha = 0.14f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                onDrawWithContent {
                    clipPath(fillPath) {
                        drawRect(brush = baseGlassBrush)
                        drawRect(brush = naturalGleamBrush)
                        drawRect(brush = topBevelBrush)
                        drawRect(brush = frostedGrainSheen)
                    }
                    drawContent()
                    drawPath(
                        path = insetPath,
                        brush = crystalRimBrush,
                        style = Stroke(width = strokePx)
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
        .clip(shape)
        .onGloballyPositioned { coordinates ->
            windowY = coordinates.positionInWindow().y
        }
        .drawWithCache {
            val w = size.width
            val h = size.height
            val strokePx = borderWidth.toPx().coerceAtLeast(1f)
            val halfStroke = strokePx / 2f
            val crPx = cornerRadius.toPx().coerceAtLeast(0f)
            val insetCr = CornerRadius((crPx - halfStroke).coerceAtLeast(0f), (crPx - halfStroke).coerceAtLeast(0f))

            val clipRoundPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(crPx, crPx)))
            }

            val deepSurfaceBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF1E2238).copy(alpha = 0.88f),
                    0.20f to Color(0xFF141728).copy(alpha = 0.84f),
                    0.52f to primaryColor.copy(alpha = if (isPlaying) 0.22f else 0.10f),
                    0.80f to Color(0xFF0C0E1A).copy(alpha = 0.92f),
                    1.00f to Color(0xFF06070E).copy(alpha = 0.96f)
                )
            )

            // انعکاس نرم نوری چندلایه
            val shimmerX = w * (0.20f + smoothScrollRatio * 0.60f)
            val glassSweepBrush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = if (isPlaying) 0.15f else 0.08f),
                    0.30f to secondaryColor.copy(alpha = 0.07f),
                    0.60f to primaryColor.copy(alpha = 0.04f),
                    0.85f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                center = Offset(shimmerX, h * 0.25f),
                radius = (w * 0.60f).coerceAtLeast(160f)
            )

            val rimBorderBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.65f),
                    0.25f to secondaryColor.copy(alpha = glowAlpha * 0.55f),
                    0.50f to Color.White.copy(alpha = 0.25f),
                    0.75f to primaryColor.copy(alpha = glowAlpha * 0.60f),
                    1.00f to Color.White.copy(alpha = 0.18f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                clipPath(clipRoundPath) {
                    drawRect(brush = deepSurfaceBrush)
                    drawRect(brush = glassSweepBrush)
                }

                drawContent()

                drawRoundRect(
                    brush = rimBorderBrush,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = androidx.compose.ui.geometry.Size(
                        (w - strokePx).coerceAtLeast(0f),
                        (h - strokePx).coerceAtLeast(0f)
                    ),
                    cornerRadius = insetCr,
                    style = Stroke(width = strokePx)
                )
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
