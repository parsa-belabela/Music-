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
                        listOf(Color(0x28FFFFFF), Color(0x10FFFFFF), Color(0x0800E5FF))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(Color(0x80FFFFFF), Color(0x3500E5FF), Color(0x18FFFFFF))
                    ),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent,
                    specularReflectionAlpha = 0.45f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(
                        listOf(Color(0x2DFFFFFF), Color(0x120A1020), Color(0x1C182642))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(
                            Color(0x90FFFFFF),
                            Color(0x5000E5FF),
                            Color(0x308B5CF6),
                            Color(0x15FFFFFF)
                        )
                    ),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent,
                    specularReflectionAlpha = 0.6f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(
                        listOf(Color(0x401A2438), Color(0x280D1525), Color(0x35060A14))
                    ),
                    borderGradient = Brush.linearGradient(
                        listOf(Color(0x75FFFFFF), Color(0x4000E5FF), Color(0x208B5CF6))
                    ),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent,
                    specularReflectionAlpha = 0.75f
                )
            }

            AppTheme.CYBER_NIGHTS -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x3000F0FF), Color(0x120F172A))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x7500F0FF), Color(0x25FF007F))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x4000F0FF), Color(0x25060F26), Color(0x35020614))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x8500F0FF), Color(0x50FF007F), Color(0x2000F0FF))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x50020B1C), Color(0x35050D20))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x6000F0FF), Color(0x35FF007F))),
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
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x45CBD5E1), Color(0x251E2330), Color(0x350F131C))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF818CF8))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x501E2536), Color(0x350F131E))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF818CF8), Color(0xFF38BDF8))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.VELVET_NOIR -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x35FFD700), Color(0x153B0746))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x85FFD700), Color(0x30A855F7))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x38FFD700), Color(0x28380844), Color(0x3015021C))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x85FFD700), Color(0x45A855F7), Color(0x20FFD700))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x4525042D), Color(0x30100115))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x70FFD700), Color(0x30701A75))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.SUNSET_RAVE -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0x35FF6D00), Color(0x18FF007F))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x80FF6D00), Color(0x40FF007F))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x38FF6D00), Color(0x253B0B32), Color(0x301A031E))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x85FF6D00), Color(0x55FF007F), Color(0x30FFD600))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x452D082A), Color(0x30140217))),
                    borderGradient = Brush.linearGradient(listOf(Color(0x70FF6D00), Color(0x35FF007F))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
            }

            AppTheme.DIGITAL_ACID -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x3039FF14), Color(0x10051205))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x8039FF14), Color(0x2500FF66))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x3039FF14), Color(0x20071C07), Color(0x28020802))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x8539FF14), Color(0x4000FF66), Color(0x2039FF14))),
                    shadowElevation = 0.dp,
                    shadowColor = Color.Transparent
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0x45081E08), Color(0x30020A02))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0x6539FF14), Color(0x30059669))),
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

