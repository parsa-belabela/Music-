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
import androidx.compose.ui.geometry.Size
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

/**
 * Material Thickness levels for Liquid Glass & Thematic Surfaces:
 * - THIN: Floating buttons, small pills, chip filters, track items
 * - REGULAR: Floating capsule MiniPlayer, navigation bar, cards, playlists
 * - THICK: Modals, bottom sheets, menus, dialogs
 */
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
                    Color(0xFF1A1A2E).copy(alpha = 0.76f),
                    accentColor.copy(alpha = baseAlpha * 0.55f),
                    Color(0xFF0B0B14).copy(alpha = 0.85f)
                )
            ),
            borderGradient = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color(0xFF00E5FF).copy(alpha = 0.35f),
                    Color(0xFF9D4EDD).copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.18f)
                )
            ),
            shadowElevation = if (thickness == GlassThickness.THICK) 8.dp else 4.dp,
            shadowColor = Color.Black.copy(alpha = 0.45f),
            specularReflectionAlpha = 0.5f
        )
    }
}

/**
 * موتور شیشه‌ای Liquid Glass با انعکاس نور زنده هنگام اسکرول، تکسچر میکرو-کریستالی و عمق سه‌بعدی
 * کاملاً خودکار و حساس به بالا و پایین رفتن صفحه در تمام لیست‌ها
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.16f,
    strokeAlpha: Float = 0.48f,
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
    val effectiveBorderWidth = borderWidth

    // موقعیت عمودی لحظه‌ای کارت در صفحه هنگام اسکرول بالا و پایین
    var windowY by remember { mutableFloatStateOf(0f) }

    // نرم‌سازی حرکت انعکاس نور با فیزیک فنری (برای حس سیال بودن شیشه مایع)
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

            // محاسبه مرکز پرتو انعکاس بر اساس اسکرول
            // وقتی اسکرول می‌کنی، پرتو نور به صورت مورب از چپ-بالا به راست-پایین روی شیشه سر می‌خورد
            val sweepProgress = (1f - smoothScrollRatio) * 1.6f - 0.3f
            val beamCenterX = w * sweepProgress
            val beamCenterY = h * (0.2f + smoothScrollRatio * 0.6f)
            val beamWidth = (w * 0.55f).coerceAtLeast(180f)

            // ۱. لایه پایه شیشه عمیق (Subsurface Deep Glass)
            val baseGlassBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1A1A2E).copy(alpha = 0.76f),
                    effectiveAccent.copy(alpha = effectiveGlassAlpha * 0.55f),
                    Color(0xFF0B0B14).copy(alpha = 0.85f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            // ۲. لایه پرتو انعکاس نور منشوری هنگام اسکرول (Specular Optical Sweep)
            val specularSweepBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.32f to secondaryAccent.copy(alpha = 0.04f),
                    0.44f to Color.White.copy(alpha = 0.14f),
                    0.50f to Color.White.copy(alpha = 0.32f), // هسته درخشان انعکاس نور
                    0.56f to effectiveAccent.copy(alpha = 0.15f),
                    0.68f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                start = Offset(beamCenterX - beamWidth, beamCenterY - beamWidth * 0.7f),
                end = Offset(beamCenterX + beamWidth, beamCenterY + beamWidth * 0.7f)
            )

            // ۳. لایه عمق سه‌بعدی سقف و کف شیشه (3D Top Catch-Light & Bottom Depth Shadow)
            val topLightIntensity = (1f - (smoothScrollRatio * 0.6f)).coerceIn(0.25f, 0.95f)
            val innerBevelBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.28f * topLightIntensity),
                    0.08f to Color.White.copy(alpha = 0.05f),
                    0.85f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.45f)
                )
            )

            // ۴. کادر دور شیشه با درخشش نقطه‌ای هماهنگ با زاویه اسکرول
            val dynamicRimBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = strokeAlpha * 0.95f),
                    0.35f to secondaryAccent.copy(alpha = strokeAlpha * 0.85f),
                    0.65f to effectiveAccent.copy(alpha = strokeAlpha * 0.90f),
                    1.0f to Color.White.copy(alpha = strokeAlpha * 0.40f)
                ),
                start = Offset(w * (1f - smoothScrollRatio), 0f),
                end = Offset(w * smoothScrollRatio, h)
            )

            onDrawWithContent {
                clipPath(outlinePath) {
                    // الف) رسم پس‌زمینه عمیق شیشه‌ای
                    drawRect(brush = baseGlassBrush)

                    // ب) رسم تکسچر میکرو-کریستالی (ایجاد بافت شیشه مات واقعی با خطوط نوری بسیار ظریف)
                    val step = 6.dp.toPx().coerceAtLeast(12f)
                    var yPos = 0f
                    while (yPos < h) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.018f),
                            start = Offset(0f, yPos),
                            end = Offset(w, yPos + (step * 0.35f)),
                            strokeWidth = 1f,
                            blendMode = BlendMode.Overlay
                        )
                        yPos += step
                    }

                    // ج) رسم انعکاس نور حساس به اسکرول روی سطح شیشه
                    drawRect(
                        brush = specularSweepBrush,
                        blendMode = BlendMode.Plus
                    )

                    // د) رسم پخ سه‌بعدی داخلی (عمق شیشه)
                    drawRect(brush = innerBevelBrush)
                }

                // رسم محتوای داخل کارت (متن، آیکون، کاور آهنگ و غیره)
                drawContent()

                // ه) رسم هایلایت لبه‌ی داخلی بالا (3D Specular Inner Rim)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f * topLightIntensity),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.35f
                    ),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx()),
                    blendMode = BlendMode.Screen
                )

                // و) رسم خط رنگی و نوری اصلی دور تا دور شیشه
                drawRoundRect(
                    brush = dynamicRimBrush,
                    cornerRadius = cr,
                    style = Stroke(width = effectiveBorderWidth.toPx().coerceAtLeast(1.35f * density.density))
                )
            }
        }
}

/**
 * کادر مستطیلی موازی مخصوص آیتم‌های آهنگ با خط رنگی نئونی + انعکاس نور زنده هنگام اسکرول بالا و پایین
 */
@Composable
fun Modifier.premiumTrackGlowingBorder(
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    isPlaying: Boolean = false,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = if (isPlaying) 1.9.dp else 1.35.dp
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
        initialValue = if (isPlaying) 0.75f else 0.55f,
        targetValue = if (isPlaying) 1.0f else 0.88f,
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

            // جابجایی پرتو انعکاس روی کارت آهنگ با اسکرول کردن لیست
            val reflectionX = w * ((1f - smoothScrollRatio) * 1.5f - 0.25f)
            val reflectionWidth = (w * 0.48f).coerceAtLeast(160f)

            val outerGlowBrush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha * 0.42f),
                    secondaryColor.copy(alpha = glowAlpha * 0.35f),
                    Color(0xFFFF2A85).copy(alpha = glowAlpha * 0.28f),
                    primaryColor.copy(alpha = glowAlpha * 0.42f)
                ),
                start = Offset(reflectionX - reflectionWidth, 0f),
                end = Offset(reflectionX + reflectionWidth, h)
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

            // پرتو انعکاس شیشه‌ای با شکست نور روی خودِ کارت آهنگ
            val glassSweepBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.35f to secondaryColor.copy(alpha = 0.05f),
                    0.46f to Color.White.copy(alpha = 0.16f),
                    0.50f to Color.White.copy(alpha = 0.30f),
                    0.54f to primaryColor.copy(alpha = 0.14f),
                    0.65f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                start = Offset(reflectionX - reflectionWidth, -h * 0.4f),
                end = Offset(reflectionX + reflectionWidth, h * 1.4f)
            )

            // خط رنگی دور تا دور مستطیل آهنگ که نقطه درخشش آن با اسکرول می‌چرخد
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
                    0.35f to secondaryColor.copy(alpha = glowAlpha * 0.95f),
                    0.50f to Color.White.copy(alpha = 0.90f), // برق افتادن روی خط دور کادر هنگام اسکرول
                    0.65f to Color(0xFFFF2A85).copy(alpha = glowAlpha * 0.85f),
                    1.00f to primaryColor.copy(alpha = glowAlpha)
                ),
                start = rimStart,
                end = rimEnd
            )

            onDrawWithContent {
                // ۱. هاله نوری بیرونی دور تا دور کادر مستطیلی آهنگ
                drawRoundRect(
                    brush = outerGlowBrush,
                    cornerRadius = cr,
                    style = Stroke(width = (borderWidth + 3.dp).toPx())
                )

                // ۲. سطح شیشه‌ای تیره با عمق بالا
                drawRoundRect(
                    brush = deepSurfaceBrush,
                    cornerRadius = cr
                )

                // ۳. تکسچر میکرو-کریستالی سطح کارت
                val grainStep = 5.dp.toPx().coerceAtLeast(10f)
                var gy = 0f
                while (gy < h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.016f),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f,
                        blendMode = BlendMode.Overlay
                    )
                    gy += grainStep
                }

                // ۴. پرتو انعکاس نور حساس به اسکرول روی شیشه
                drawRoundRect(
                    brush = glassSweepBrush,
                    cornerRadius = cr,
                    blendMode = BlendMode.Plus
                )

                // ۵. پخ نوری سه‌بعدی لبه بالایی داخل کارت (Depth Bevel)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx())
                )

                // رسم محتوای کارت آهنگ (عکس، نام آهنگ، آیکون ایرپاد/اسپیکر)
                drawContent()

                // ۶. خط رنگی درخشان اصلی دور تا دور مستطیل آهنگ
                drawRoundRect(
                    brush = rimBorderBrush,
                    cornerRadius = cr,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
        }
        .clip(shape)
}

/**
 * تابع کمکی برای کادرهای موازی نئونی
 */
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
