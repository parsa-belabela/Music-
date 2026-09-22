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
        return when (theme) {
            AppTheme.PURE_LIQUID_GLASS -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(
                        listOf(Color(0x0EFFFFFF), Color(0x05FFFFFF), Color(0x0400E5FF))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(Color(0x30FFFFFF), Color(0x1800E5FF), Color(0x0EFFFFFF))
                    ),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent,
                    specularReflectionAlpha = 0.2f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(
                        listOf(Color(0xF0121728), Color(0xEA0B101E), Color(0xEE10182A))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(
                            Color(0x80FFFFFF),
                            Color(0x4000E5FF),
                            Color(0x258B5CF6),
                            Color(0x15FFFFFF)
                        )
                    ),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f),
                    specularReflectionAlpha = 0.5f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(
                        listOf(Color(0xF5141B2D), Color(0xEE0A0F1D), Color(0xF2060A14))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(Color(0x70FFFFFF), Color(0x3500E5FF), Color(0x1A8B5CF6))
                    ),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f),
                    specularReflectionAlpha = 0.65f
                )
            }

            AppTheme.CYBER_NIGHTS -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x1000F0FF), Color(0x050F172A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x3500F0FF), Color(0x15FF007F))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF0081224), Color(0xEA050D1C), Color(0xEE030814))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x8000F0FF), Color(0x45FF007F), Color(0x1A00F0FF))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f)
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF5040C1A), Color(0xEE030814))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x6000F0FF), Color(0x30FF007F))),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f)
                )
            }

            AppTheme.Y2K_CHROME -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0x10E2E8F0), Color(0x0594A3B8))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x2038BDF8), Color(0x1264748B))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF0181F2C), Color(0xEA121724), Color(0xEE0D111A))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x90E2E8F0), Color(0x4538BDF8), Color(0x25818CF8))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f)
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF5141924), Color(0xEE0B0E16))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x80CBD5E1), Color(0x35818CF8), Color(0x2038BDF8))),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f)
                )
            }

            AppTheme.VELVET_NOIR -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x10FFD700), Color(0x053B0746))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x3AFFD700), Color(0x18A855F7))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF01E0624), Color(0xEA15031A), Color(0xEE0F0112))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x80FFD700), Color(0x40A855F7), Color(0x1AFFD700))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f)
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF518041E), Color(0xEE0B010E))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x65FFD700), Color(0x28701A75))),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f)
                )
            }

            AppTheme.SUNSET_RAVE -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0x10FF6D00), Color(0x05FF007F))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x35FF6D00), Color(0x18FF007F))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF020081C), Color(0xEA160414), Color(0xEE0F020E))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x80FF6D00), Color(0x45FF007F), Color(0x20FFD600))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f)
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF51A0518), Color(0xEE0D020C))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x65FF6D00), Color(0x25FF007F))),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f)
                )
            }

            AppTheme.DIGITAL_ACID -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x1039FF14), Color(0x05051205))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x3539FF14), Color(0x1200FF66))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF0061506), Color(0xEA030E03), Color(0xEE010701))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x8039FF14), Color(0x3500FF66), Color(0x1A39FF14))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black.copy(alpha = 0.45f)
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xF5041004), Color(0xEE010601))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x5539FF14), Color(0x20059669))),
                    shadowElevation = 16.dp,
                    shadowColor = Color.Black.copy(alpha = 0.6f)
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
                AppTheme.PURE_LIQUID_GLASS -> {
                    // Pure crystal diagonal light refraction and top specular highlight
                    if (size.width > 20f && size.height > 20f) {
                        // Top crystal edge highlight
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x99FFFFFF),
                                    Color(0x6000E5FF),
                                    Color(0x80FFFFFF),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.5f
                        )
                    }
                }
                AppTheme.CYBER_NIGHTS -> {
                    // Subtle cyber horizon neon line
                    if (size.height > 60f) {
                        drawLine(
                            color = Color(0x3500F0FF),
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
                            color = Color(0x60FFD700),
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
                            color = Color(0x60FF6D00),
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
                            color = Color(0x7039FF14),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.8f
                        )
                    }
                }
            }

            // Draw foreground content (text, artwork, controls) cleanly on top
            drawContent()
        }
}

