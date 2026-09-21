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
 * Material Thickness levels for Liquid Glass in iOS 26 philosophy:
 * - THIN: Small floating buttons, chip pills, micro controls
 * - REGULAR: Floating capsule MiniPlayer, navigation bar, playback controls
 * - THICK: Modals, bottom sheets, menus, high-readability text panels
 */
enum class GlassThickness {
    THIN,
    REGULAR,
    THICK
}

/**
 * Liquid Glass Material specification
 */
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
                        Color(0x28FFFFFF),
                        Color(0x0CFFFFFF)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x45FFFFFF),
                        Color(0x10FFFFFF)
                    )
                ),
                shadowElevation = 8.dp,
                shadowColor = accentColor.copy(alpha = 0.15f),
                specularReflectionAlpha = 0.22f
            )

            GlassThickness.REGULAR -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x24202838),
                        Color(0x18121622)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x38FFFFFF),
                        accentColor.copy(alpha = 0.28f),
                        Color(0x0EFFFFFF)
                    )
                ),
                shadowElevation = 16.dp,
                shadowColor = Color.Black.copy(alpha = 0.45f),
                specularReflectionAlpha = 0.18f
            )

            GlassThickness.THICK -> LiquidGlassSpec(
                surfaceGradient = Brush.verticalGradient(
                    listOf(
                        Color(0xF013141F),
                        Color(0xFA0B0C14)
                    )
                ),
                borderGradient = Brush.verticalGradient(
                    listOf(
                        Color(0x2AFFFFFF),
                        Color(0x0DFFFFFF)
                    )
                ),
                shadowElevation = 28.dp,
                shadowColor = Color.Black.copy(alpha = 0.65f),
                specularReflectionAlpha = 0.12f
            )
        }
    }
}

/**
 * Modifier for applying Liquid Glass effect with layered specular highlights,
 * delicate border lighting, dynamic tint and soft diffuse shadow.
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
            ambientColor = if (tintColor != Color.Transparent) tintColor else spec.shadowColor,
            spotColor = spec.shadowColor
        )
        .clip(shape)
        .background(
            brush = if (tintColor != Color.Transparent) {
                Brush.verticalGradient(
                    listOf(
                        tintColor.copy(alpha = (tintAlpha * 1.6f).coerceIn(0.05f, 0.45f)),
                        Color(0xD00E0E18)
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
            // Subtle top-edge light reflection / specular sheen
            if (spec.specularReflectionAlpha > 0f) {
                val highlightHeight = size.height * 0.35f
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
