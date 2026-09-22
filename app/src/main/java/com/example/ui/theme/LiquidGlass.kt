package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme

val LocalAppTheme = compositionLocalOf { AppTheme.CYBER_NIGHTS }

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
    val specularReflectionAlpha: Float = 0.0f
)

object LiquidGlassDesign {

    fun getSpec(
        thickness: GlassThickness,
        accentColor: Color = Color.White,
        theme: AppTheme = AppTheme.CYBER_NIGHTS
    ): LiquidGlassSpec {
        return when (theme) {
            AppTheme.CYBER_NIGHTS -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x2806B6D4), Color(0x100F172A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x5506B6D4), Color(0x15D946EF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xCC0E172A), Color(0xF0070B14))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x5506B6D4), accentColor.copy(alpha = 0.35f), Color(0x20D946EF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF50B1220), Color(0xFA050811))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x4006B6D4), Color(0x20D946EF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.Y2K_CHROME -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0x35E2E8F0), Color(0x1594A3B8))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF38BDF8), Color(0xFF64748B))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xD01E2330), Color(0xF00F131C))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF818CF8))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF2151922), Color(0xFA0A0D14))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF818CF8), Color(0xFF38BDF8))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.VELVET_NOIR -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x35701A75), Color(0x152E0854))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x60F59E0B), Color(0x20701A75))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xD5240E2B), Color(0xF2120617))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x55F59E0B), Color(0x30A855F7), Color(0x15F59E0B))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF51C0822), Color(0xFC0A030D))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x50F59E0B), Color(0x25701A75))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.SUNSET_RAVE -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0x35F97316), Color(0x15EC4899))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x65F97316), Color(0x30A855F7))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xD02E1428), Color(0xF018081E))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x60F97316), Color(0x40EC4899), Color(0x25A855F7))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF5240D20), Color(0xFC120414))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x50F97316), Color(0x25EC4899))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.DIGITAL_ACID -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x2884CC16), Color(0x10050805))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x6584CC16), Color(0x2015803D))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xD00A1208), Color(0xF2040703))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x5584CC16), Color(0x3022C55E), Color(0x1584CC16))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF5060C05), Color(0xFC020402))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x4584CC16), Color(0x2015803D))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.MINIMAL_STUDIO -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x25FFFFFF), Color(0x0CFFFFFF))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x15FFFFFF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xD01C1C24), Color(0xF0121218))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x12FFFFFF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF514141B), Color(0xFC0A0A0F))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x10FFFFFF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }
        }
    }
}

/**
 * Modifier for applying Liquid Glass & Theme-specific tactile textured effects:
 * - Crystal glassmorphism with specular reflections
 * - 3D Lego brick studs & bevels
 * - Cartoon / Anime ink outlines and comic halftone dots
 * - Y2K Cyber Chrome metallic scanlines and holographic edges
 * - Synthwave retro grid lines and sunset neon glow
 * - Obsidian Matrix digital code nodes & phosphor lasers
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    thickness: GlassThickness = GlassThickness.REGULAR,
    tintColor: Color = Color.Transparent,
    tintAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp,
    appTheme: AppTheme? = null
): Modifier = composed {
    val activeTheme = appTheme ?: LocalAppTheme.current
    val spec = LiquidGlassDesign.getSpec(thickness, tintColor, activeTheme)

    this
        .clip(shape)
        .background(
            brush = if (tintColor != Color.Transparent && activeTheme == AppTheme.CYBER_NIGHTS) {
                Brush.verticalGradient(
                    listOf(
                        tintColor.copy(alpha = (tintAlpha * 1.5f).coerceIn(0.08f, 0.45f)),
                        Color(0xF0080E1A)
                    )
                )
            } else {
                spec.surfaceGradient
            },
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = spec.borderGradient,
            shape = shape
        )
        .drawWithContent {
            // Tactile theme textures rendered safely behind content
            when (activeTheme) {
                AppTheme.CYBER_NIGHTS -> {
                    // Subtle cyber horizon neon line
                    if (size.height > 60f) {
                        drawLine(
                            color = Color(0x2206B6D4),
                            start = Offset(12f, 0f),
                            end = Offset(size.width - 12f, 0f),
                            strokeWidth = 1.5f
                        )
                    }
                }
                AppTheme.Y2K_CHROME -> {
                    // Chrome metallic horizontal sheen lines
                    if (size.height > 24f) {
                        val gap = 6.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color(0x0C38BDF8),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += gap
                        }
                    }
                }
                AppTheme.VELVET_NOIR -> {
                    // Warm gold luxury edge accent
                    if (size.width > 20f) {
                        drawLine(
                            color = Color(0x30F59E0B),
                            start = Offset(size.width * 0.15f, 0f),
                            end = Offset(size.width * 0.85f, 0f),
                            strokeWidth = 1.2f
                        )
                    }
                }
                AppTheme.SUNSET_RAVE -> {
                    // Solar sunset dusk line
                    if (size.height > 40f) {
                        drawLine(
                            color = Color(0x30F97316),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.5f
                        )
                    }
                }
                AppTheme.DIGITAL_ACID -> {
                    // Neon lime laser micro-edge
                    if (size.width > 30f) {
                        drawLine(
                            color = Color(0x5084CC16),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.8f
                        )
                    }
                }
                AppTheme.MINIMAL_STUDIO -> {
                    // Clean surgical border, zero distracting noise
                }
            }

            // Draw foreground content (text, artwork, controls) cleanly on top
            drawContent()
        }
}

