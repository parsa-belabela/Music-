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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme

/**
 * Material Thickness levels for Liquid Glass & Thematic Surfaces:
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
        accentColor: Color = Color.White,
        theme: AppTheme = AppTheme.GLASS
    ): LiquidGlassSpec {
        return when (theme) {
            AppTheme.GLASS -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x10FFFFFF))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x15FFFFFF))),
                    shadowElevation = 8.dp,
                    shadowColor = accentColor.copy(alpha = 0.14f),
                    specularReflectionAlpha = 0.28f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x35283248), Color(0x1F121828))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x45FFFFFF), accentColor.copy(alpha = 0.35f), Color(0x12FFFFFF))),
                    shadowElevation = 14.dp,
                    shadowColor = Color.Black.copy(alpha = 0.38f),
                    specularReflectionAlpha = 0.22f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xEB131626), Color(0xF50C0E1A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x10FFFFFF))),
                    shadowElevation = 24.dp,
                    shadowColor = Color.Black.copy(alpha = 0.55f),
                    specularReflectionAlpha = 0.15f
                )
            }

            AppTheme.LEGO -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFFE52521), Color(0xFFB81D1A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFFFF7A78), Color(0xFF6B0E0C))),
                    shadowElevation = 10.dp,
                    shadowColor = Color(0x80000000),
                    specularReflectionAlpha = 0.35f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF26262B), Color(0xFF18181A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF4A4A54), Color(0xFF0F0F12))),
                    shadowElevation = 12.dp,
                    shadowColor = Color(0x99000000),
                    specularReflectionAlpha = 0.30f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1E1E22), Color(0xFF121214))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF555562), Color(0xFF08080A))),
                    shadowElevation = 20.dp,
                    shadowColor = Color(0xDD000000),
                    specularReflectionAlpha = 0.25f
                )
            }

            AppTheme.CARTOON -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.40f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF231C38), Color(0xFF171224))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 12.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.30f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1D172E), Color(0xFF100D1A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 18.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.20f
                )
            }

            AppTheme.CYBER_CHROME -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF64748B))),
                    shadowElevation = 10.dp,
                    shadowColor = Color(0x6038BDF8),
                    specularReflectionAlpha = 0.45f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1E222D), Color(0xFF0F1219))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF94A3B8))),
                    shadowElevation = 14.dp,
                    shadowColor = Color(0x80000000),
                    specularReflectionAlpha = 0.38f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF161A24), Color(0xFF0B0D13))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF818CF8), Color(0xFF475569))),
                    shadowElevation = 22.dp,
                    shadowColor = Color(0xCC000000),
                    specularReflectionAlpha = 0.30f
                )
            }

            AppTheme.VAPORWAVE -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFF06B6D4))),
                    shadowElevation = 10.dp,
                    shadowColor = Color(0x66EC4899),
                    specularReflectionAlpha = 0.35f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF28113B), Color(0xFF160824))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF06B6D4))),
                    shadowElevation = 14.dp,
                    shadowColor = Color(0x88EC4899),
                    specularReflectionAlpha = 0.28f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1F0B30), Color(0xFF10041B))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFD946EF), Color(0xFF06B6D4))),
                    shadowElevation = 20.dp,
                    shadowColor = Color(0xAAEC4899),
                    specularReflectionAlpha = 0.22f
                )
            }

            AppTheme.OBSIDIAN_MATRIX -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF0B1914), Color(0xFF050D0A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF00FF88), Color(0xFF00552E))),
                    shadowElevation = 8.dp,
                    shadowColor = Color(0x6000FF88),
                    specularReflectionAlpha = 0.30f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF0D1412), Color(0xFF060A08))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF), Color(0xFF032617))),
                    shadowElevation = 12.dp,
                    shadowColor = Color(0x4000FF88),
                    specularReflectionAlpha = 0.25f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF090E0C), Color(0xFF030504))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF00FF88), Color(0xFF00381E))),
                    shadowElevation = 20.dp,
                    shadowColor = Color(0x8000FF88),
                    specularReflectionAlpha = 0.20f
                )
            }
        }
    }
}

/**
 * Modifier for applying Liquid Glass & Theme-specific tactile textured effects:
 * - Crystal glassmorphism
 * - 3D Lego brick studs & bevels
 * - Cartoon / Anime ink outlines
 * - Y2K Cyber Chrome liquid metallic sheen
 * - Synthwave sunset dusk glow
 * - Obsidian Matrix carbon laser borders
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp,
    appTheme: AppTheme = AppTheme.GLASS
): Modifier = composed {
    val spec = LiquidGlassDesign.getSpec(thickness, tintColor, appTheme)
    val effectiveBorderWidth = if (appTheme == AppTheme.CARTOON) 2.2.dp else borderWidth

    this
        .shadow(
            elevation = spec.shadowElevation,
            shape = shape,
            ambientColor = if (tintColor != Color.Transparent) tintColor.copy(alpha = 0.25f) else spec.shadowColor,
            spotColor = spec.shadowColor
        )
        .clip(shape)
        .background(
            brush = if (tintColor != Color.Transparent && appTheme == AppTheme.GLASS) {
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
            width = effectiveBorderWidth,
            brush = spec.borderGradient,
            shape = shape
        )
        .drawWithContent {
            drawContent()

            // 1. Lego 3D Brick Studs tactile pattern overlay
            if (appTheme == AppTheme.LEGO && size.width > 30f && size.height > 30f) {
                val studSpacing = 24.dp.toPx()
                val studRadius = 4.5.dp.toPx()
                var x = studSpacing / 2f
                while (x < size.width) {
                    var y = studSpacing / 2f
                    while (y < size.height) {
                        // Embossed 3D shadow & highlight for each stud
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.35f),
                            radius = studRadius,
                            center = Offset(x + 1f, y + 1.5f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = studRadius,
                            center = Offset(x, y)
                        )
                        y += studSpacing
                    }
                    x += studSpacing
                }
            }

            // 2. Top-edge light reflection / specular sheen
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
                    size = Size(size.width, highlightHeight)
                )
            }

            // 3. Obsidian Matrix laser micro-grid accent
            if (appTheme == AppTheme.OBSIDIAN_MATRIX && size.height > 40f) {
                drawLine(
                    color = Color(0x4000FF88),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.4f, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
}

