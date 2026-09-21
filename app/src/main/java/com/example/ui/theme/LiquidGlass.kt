package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Thickness levels for Liquid Glass:
 * - THIN: Floating buttons, small pills, chip filters
 * - REGULAR: Floating capsule MiniPlayer, navigation bar, cards
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
    val specularReflectionAlpha: Float
)

object LiquidGlassDesign {

    fun getSpec(
        thickness: GlassThickness,
        accentColor: Color = Color.White
    ): LiquidGlassSpec {
        return when (thickness) {
            GlassThickness.THIN -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x30FFFFFF),
                        Color(0x10FFFFFF)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x55FFFFFF),
                        Color(0x15FFFFFF)
                    )
                ),
                shadowElevation = 8.dp,
                shadowColor = accentColor.copy(alpha = 0.12f),
                specularReflectionAlpha = 0.28f
            )

            GlassThickness.REGULAR -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x35283248),
                        Color(0x1F121828)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x45FFFFFF),
                        accentColor.copy(alpha = 0.35f),
                        Color(0x12FFFFFF)
                    )
                ),
                shadowElevation = 14.dp,
                shadowColor = Color.Black.copy(alpha = 0.38f),
                specularReflectionAlpha = 0.22f
            )

            GlassThickness.THICK -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xEB131626),
                        Color(0xF50C0E1A)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x38FFFFFF),
                        Color(0x10FFFFFF)
                    )
                ),
                shadowElevation = 24.dp,
                shadowColor = Color.Black.copy(alpha = 0.55f),
                specularReflectionAlpha = 0.15f
            )
        }
    }
}

/**
 * Modifier for applying Liquid Glass effect with crystal translucency,
 * delicate border illumination, dynamic tint and soft specular highlight.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp
): Modifier = composed {
    val spec = LiquidGlassDesign.getSpec(thickness, tintColor)

    this
        .shadow(
            elevation = spec.shadowElevation,
            shape = shape,
            ambientColor = if (tintColor != Color.Transparent) tintColor.copy(alpha = 0.25f) else spec.shadowColor,
            spotColor = spec.shadowColor
        )
        .clip(shape)
        .background(
            brush = if (tintColor != Color.Transparent) {
                Brush.verticalGradient(
                    listOf(
                        tintColor.copy(alpha = (tintAlpha * 1.5f).coerceIn(0.06f, 0.40f)),
                        Color(0x950E101D)
                    )
                )
            } else {
                spec.surfaceGradient
            }
        )
        .border(
            width = borderWidth,
            brush = spec.borderGradient,
            shape = shape
        )
        .drawWithContent {
            drawContent()
            // Top-edge light reflection / glass specular sheen
            if (spec.specularReflectionAlpha > 0f) {
                val highlightHeight = (size.height * 0.35f).coerceAtLeast(12f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = spec.specularReflectionAlpha),
                            Color.White.copy(alpha = 0f)
                        ),
                        startY = 0f,
                        endY = highlightHeight
                    ),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width, highlightHeight)
                )
            }
        }
}
