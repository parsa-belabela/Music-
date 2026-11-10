package com.example.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme

/**
 * Retained for full binary & source compatibility with existing components.
 * Sensor registration is kept no-op by default to eliminate battery drain and unnecessary UI jitter.
 */
object HardwareGyroscopeManager : SensorEventListener {
    var roll by mutableFloatStateOf(0f)
        private set
    var pitch by mutableFloatStateOf(0f)
        private set

    fun acquire(context: Context) = Unit
    fun release() = Unit
    override fun onSensorChanged(event: SensorEvent?) = Unit
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
fun rememberGyroscopeSensorActive() = Unit

@Composable
fun rememberGyroscopeTilt(): Pair<Float, Float> = 0f to 0f

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
        return when (thickness) {
            GlassThickness.THIN -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(Color(0xFF181818), Color(0xFF141414))
                ),
                borderGradient = Brush.linearGradient(
                    listOf(Color(0x12FFFFFF), Color(0x08FFFFFF))
                ),
                shadowElevation = 0.dp,
                shadowColor = Color.Transparent,
                specularReflectionAlpha = 0f
            )
            GlassThickness.REGULAR -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(Color(0xFF202020), Color(0xFF181818))
                ),
                borderGradient = Brush.linearGradient(
                    listOf(Color(0x16FFFFFF), Color(0x0AFFFFFF))
                ),
                shadowElevation = 2.dp,
                shadowColor = Color.Black.copy(alpha = 0.35f),
                specularReflectionAlpha = 0f
            )
            GlassThickness.THICK -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(Color(0xFF262626), Color(0xFF1C1C1C))
                ),
                borderGradient = Brush.linearGradient(
                    listOf(Color(0x1FFFFFFF), Color(0x0FFFFFFF))
                ),
                shadowElevation = 4.dp,
                shadowColor = Color.Black.copy(alpha = 0.45f),
                specularReflectionAlpha = 0f
            )
        }
    }
}

/**
 * Redesigned to deliver a clean, Spotify-grade matte surface with subtle tonal elevation
 * and zero heavy neon/rainbow border clutter or scroll-position recomposition overhead.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(12.dp),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.08f,
    borderWidth: Dp = 0.5.dp,
    appTheme: AppTheme? = null,
    accentColor: Color = tintColor,
    secondaryAccent: Color = Color.Transparent
): Modifier = composed {
    val baseColor = when (thickness) {
        GlassThickness.THIN -> Color(0xFF181818)
        GlassThickness.REGULAR -> Color(0xFF202020)
        GlassThickness.THICK -> Color(0xFF262626)
    }

    val effectiveSurface = remember(baseColor, tintColor, tintAlpha) {
        if (tintColor != Color.Transparent && tintColor != Color.Unspecified && tintAlpha > 0.01f) {
            val clampedRatio = (tintAlpha * 0.25f).coerceIn(0.02f, 0.12f)
            lerp(baseColor, tintColor.copy(alpha = 1f), clampedRatio)
        } else {
            baseColor
        }
    }

    val subtleBorderColor = remember(thickness) {
        when (thickness) {
            GlassThickness.THIN -> Color(0x0DFFFFFF)
            GlassThickness.REGULAR -> Color(0x14FFFFFF)
            GlassThickness.THICK -> Color(0x1FFFFFFF)
        }
    }

    this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }
            val strokePx = borderWidth.coerceAtMost(1.dp).toPx()

            onDrawWithContent {
                drawPath(path = outlinePath, color = effectiveSurface)
                drawContent()
                if (strokePx > 0f) {
                    drawPath(
                        path = outlinePath,
                        color = subtleBorderColor,
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }
}

@Composable
fun Modifier.premiumTrackGlowingBorder(
    primaryColor: Color = AuraSpotifyGreen,
    secondaryColor: Color = AuraSpotifyGreen,
    isPlaying: Boolean = false,
    cornerRadius: Dp = 8.dp,
    borderWidth: Dp = if (isPlaying) 1.dp else 0.dp
): Modifier = composed {
    this.liquidGlass(
        shape = RoundedCornerShape(cornerRadius),
        thickness = if (isPlaying) GlassThickness.REGULAR else GlassThickness.THIN,
        tintColor = if (isPlaying) primaryColor else Color.Transparent,
        tintAlpha = if (isPlaying) 0.15f else 0f,
        borderWidth = borderWidth
    )
}

