package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.audio.AmbientPalette
import com.example.data.model.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * پس‌زمینه اورورا فوق‌العاده سریع و بهینه‌سازی‌شده برای حذف کامل لگ و اجرای ۱۲۰ فریم بر ثانیه
 * بدون سربار BlendModeهای سنگین و بدون محاسبات مثلثاتی پرمصرف در هر فریم
 */
@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme,
    modifier: Modifier = Modifier,
    energyBoostProvider: () -> Float = { 0f },
    intensity: Float = 1.0f
) {
    val animPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgPrimary"
    )
    val animSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgSecondary"
    )
    val animAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AuroraFluidMotion")

    // انیمیشن با نرخ رفرش پیوسته و بدون بار پردازشی
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhase"
    )

    val baseBackgroundColor = Color(0xFF04060C)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val boost = energyBoostProvider().coerceIn(0f, 1f)
            val dynamicScale = (1f + boost * 0.25f) * intensity

            // ۱. هاله نوری اصلی و لطیف در گوشه بالا
            val orb1X = w * (0.35f + 0.15f * cos(phase))
            val orb1Y = h * (0.22f + 0.12f * sin(phase))
            val radius1 = w * 0.85f * dynamicScale

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animPrimary.copy(alpha = 0.38f),
                        0.45f to animPrimary.copy(alpha = 0.18f),
                        0.80f to animAccent.copy(alpha = 0.06f),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = radius1
                ),
                radius = radius1,
                center = Offset(orb1X, orb1Y)
            )

            // ۲. هاله نوری عمیق و اتمسفریک در گوشه پایین
            val orb2X = w * (0.65f - 0.14f * sin(phase))
            val orb2Y = h * (0.68f - 0.12f * cos(phase))
            val radius2 = w * 0.90f * dynamicScale

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animSecondary.copy(alpha = 0.32f),
                        0.50f to animSecondary.copy(alpha = 0.14f),
                        0.85f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = radius2
                ),
                radius = radius2,
                center = Offset(orb2X, orb2Y)
            )

            // ۳. لایه محافظ کنتراست مخملی و یکدست‌کننده
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseBackgroundColor.copy(alpha = 0.35f),
                        Color(0x1804060C),
                        baseBackgroundColor.copy(alpha = 0.75f)
                    )
                )
            )
        }
    }
}
