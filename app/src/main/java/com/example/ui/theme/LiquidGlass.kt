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

val LocalAppTheme = compositionLocalOf { AppTheme.GLASS }

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
                    surfaceGradient = if (accentColor != Color.Transparent && accentColor != Color.White) {
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.85f)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFE51D24), Color(0xFFB81318)))
                    },
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFFFF8286), Color(0xFF380204))),
                    shadowElevation = 10.dp,
                    shadowColor = Color(0x99000000),
                    specularReflectionAlpha = 0.38f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF24262E), Color(0xFF16171B))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF666B80), Color(0xFF282A34), Color(0xFF0C0D10))),
                    shadowElevation = 14.dp,
                    shadowColor = Color(0xCC000000),
                    specularReflectionAlpha = 0.28f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1D1F26), Color(0xFF101114))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF7A8098), Color(0xFF20222A), Color(0xFF08090C))),
                    shadowElevation = 22.dp,
                    shadowColor = Color(0xF0000000),
                    specularReflectionAlpha = 0.22f
                )
            }

            AppTheme.CARTOON -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFFFF4757), Color(0xFFFF2E44))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 8.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.40f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF2B2144), Color(0xFF1C152D))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 12.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.30f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF221A37), Color(0xFF130E20))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
                    shadowElevation = 18.dp,
                    shadowColor = Color.Black,
                    specularReflectionAlpha = 0.20f
                )
            }

            AppTheme.CYBER_CHROME -> when (thickness) {
                GlassThickness.THIN -> LiquidGlassSpec(
                    surfaceGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF38BDF8), Color(0xFF64748B))),
                    shadowElevation = 10.dp,
                    shadowColor = Color(0x6038BDF8),
                    specularReflectionAlpha = 0.45f
                )
                GlassThickness.REGULAR -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1E2330), Color(0xFF0F131C))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF818CF8))),
                    shadowElevation = 14.dp,
                    shadowColor = Color(0x80000000),
                    specularReflectionAlpha = 0.38f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF151922), Color(0xFF0A0D14))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF818CF8), Color(0xFF38BDF8))),
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
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF2C1342), Color(0xFF180927))),
                    borderGradient = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF06B6D4))),
                    shadowElevation = 14.dp,
                    shadowColor = Color(0x88EC4899),
                    specularReflectionAlpha = 0.28f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF210C33), Color(0xFF12041D))),
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
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF0D1613), Color(0xFF060B09))),
                    borderGradient = Brush.verticalGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF), Color(0xFF032617))),
                    shadowElevation = 12.dp,
                    shadowColor = Color(0x4000FF88),
                    specularReflectionAlpha = 0.25f
                )
                GlassThickness.THICK -> LiquidGlassSpec(
                    surfaceGradient = Brush.verticalGradient(listOf(Color(0xFF09100D), Color(0xFF030605))),
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
    val effectiveBorderWidth = if (activeTheme == AppTheme.CARTOON) 2.4.dp else borderWidth

    this
        .shadow(
            elevation = spec.shadowElevation,
            shape = shape,
            ambientColor = if (tintColor != Color.Transparent) tintColor.copy(alpha = 0.25f) else spec.shadowColor,
            spotColor = spec.shadowColor
        )
        .clip(shape)
        .background(
            brush = if (tintColor != Color.Transparent && activeTheme == AppTheme.GLASS) {
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
            // A. TACTILE THEME BACKGROUND TEXTURES (Rendered BEHIND content so text/controls are 100% visible)

            // 1. LEGO Cinematic Theme: Authentic Multi-Colored Molded Plastic Geometry, Brick Plates & Studs
            if (activeTheme == AppTheme.LEGO) {
                val legoPalette = listOf(
                    Color(0xFFE51D24), // LEGO Classic Red
                    Color(0xFFFFD500), // LEGO Bright Yellow
                    Color(0xFF0055BF), // LEGO Classic Royal Blue
                    Color(0xFF00A33B), // LEGO Classic Green
                    Color(0xFFFF6F00), // LEGO Vibrant Orange
                    Color(0xFF00A3DA), // LEGO Light Azure
                    Color(0xFF8A151B), // LEGO Deep Burgundy
                    Color(0xFF2A2D38), // LEGO Dark Stone Grey
                    Color(0xFF434958)  // LEGO Medium Stone Grey
                )

                // a) Molded Horizontal Brick Construction Seams & Colored Brick Modules
                if (size.height > 48f) {
                    val seamStep = 44.dp.toPx()
                    var seamY = seamStep
                    var seamIndex = 0
                    while (seamY < size.height - 6f) {
                        val accentColor = legoPalette[seamIndex % legoPalette.size]
                        // Subtle colored modular brick accent line
                        drawLine(
                            color = accentColor.copy(alpha = 0.25f),
                            start = Offset(6.dp.toPx(), seamY - 1.dp.toPx()),
                            end = Offset(size.width - 6.dp.toPx(), seamY - 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Shadow groove
                        drawLine(
                            color = Color.Black.copy(alpha = 0.55f),
                            start = Offset(4.dp.toPx(), seamY),
                            end = Offset(size.width - 4.dp.toPx(), seamY),
                            strokeWidth = 1.4.dp.toPx()
                        )
                        // Molded plastic light catch
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(4.dp.toPx(), seamY + 1.2.dp.toPx()),
                            end = Offset(size.width - 4.dp.toPx(), seamY + 1.2.dp.toPx()),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        seamY += seamStep
                        seamIndex++
                    }
                }

                // b) Colorful Molded ABS Circular Studs (Rendered in background with balanced depth)
                if (size.width > 28f && size.height > 28f) {
                    val studSpacing = 26.dp.toPx()
                    val studRadius = 4.0.dp.toPx()
                    val innerCavity = 1.8.dp.toPx()

                    var col = 0
                    var x = studSpacing / 2f
                    while (x < size.width) {
                        var row = 0
                        var y = studSpacing / 2f
                        while (y < size.height) {
                            // Pick an authentic LEGO color for each individual stud in the structure
                            val colorIdx = ((col * 3) + (row * 5) + col + row) % legoPalette.size
                            val studColor = legoPalette[colorIdx]

                            // 1. Ambient Occlusion Drop Shadow (Bottom-Right)
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.40f),
                                radius = studRadius + 1.2f,
                                center = Offset(x + 1.2f, y + 1.6f)
                            )
                            // 2. Plastic Stud Outer Cylinder Body in authentic LEGO Color
                            drawCircle(
                                color = studColor.copy(alpha = 0.45f),
                                radius = studRadius,
                                center = Offset(x, y)
                            )
                            // 3. Molded Bevel Top-Left Light Highlight Arc
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f),
                                radius = studRadius * 0.78f,
                                center = Offset(x - 0.7f, y - 0.7f)
                            )
                            // 4. Stud Top Inner Ring
                            drawCircle(
                                color = studColor.copy(alpha = 0.60f),
                                radius = studRadius * 0.58f,
                                center = Offset(x, y)
                            )
                            // 5. Subtle Stud Inner Center Cavity
                            drawCircle(
                                color = Color(0xFF101116),
                                radius = innerCavity,
                                center = Offset(x, y)
                            )

                            y += studSpacing
                            row++
                        }
                        x += studSpacing
                        col++
                    }

                    // Soft background scrim to ensure foreground text, lyrics & controls have 100% crisp readability
                    drawRect(
                        color = Color(0xFF101115).copy(alpha = 0.28f),
                        size = size
                    )
                }
            }

            // 2. CARTOON Theme: Pop Comic Halftone Dots
            if (activeTheme == AppTheme.CARTOON && size.width > 20f && size.height > 20f) {
                val dotSpacing = 16.dp.toPx()
                val dotRadius = 1.6.dp.toPx()
                var x = dotSpacing / 2f
                while (x < size.width) {
                    var y = dotSpacing / 2f
                    while (y < size.height) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += dotSpacing
                    }
                    x += dotSpacing
                }

                // Comic style dynamic specular highlight slash
                val slashPath = Path().apply {
                    moveTo(size.width * 0.65f, 0f)
                    lineTo(size.width * 0.82f, 0f)
                    lineTo(size.width * 0.72f, size.height * 0.35f)
                    lineTo(size.width * 0.55f, size.height * 0.35f)
                    close()
                }
                drawPath(path = slashPath, color = Color.White.copy(alpha = 0.12f))
            }

            // 3. CYBER CHROME Theme: Scanline Matrix & Iridescent Reflections
            if (activeTheme == AppTheme.CYBER_CHROME && size.height > 20f) {
                val scanlineGap = 5.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color(0x1838BDF8),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += scanlineGap
                }
            }

            // 4. VAPORWAVE Theme: Retro Perspective Grid & Sunset Horizon Glow
            if (activeTheme == AppTheme.VAPORWAVE && size.height > 40f) {
                val gridStartY = size.height * 0.55f
                var lineY = gridStartY
                var step = 5.dp.toPx()
                while (lineY < size.height) {
                    drawLine(
                        color = Color(0x30EC4899),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 1.2f
                    )
                    lineY += step
                    step *= 1.3f
                }
            }

            // 5. OBSIDIAN MATRIX Theme: Phosphor Laser Micro-Nodes & Top Laser Edge
            if (activeTheme == AppTheme.OBSIDIAN_MATRIX && size.height > 30f) {
                drawLine(
                    color = Color(0x8000FF88),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.8.dp.toPx()
                )
                val nodeSpacing = 28.dp.toPx()
                var x = 12.dp.toPx()
                while (x < size.width) {
                    var y = 14.dp.toPx()
                    while (y < size.height) {
                        drawCircle(
                            color = Color(0x2800FF88),
                            radius = 1.2.dp.toPx(),
                            center = Offset(x, y)
                        )
                        y += nodeSpacing
                    }
                    x += nodeSpacing
                }
            }

            // B. DRAW CONTENT (Text, Artwork, Lyrics, Sliders, Buttons) CLEANLY ON TOP
            drawContent()

            // C. SUBTLE SPECULAR TOP-EDGE LIGHT SHEEN
            if (spec.specularReflectionAlpha > 0f) {
                val highlightHeight = (size.height * 0.28f).coerceAtLeast(8f)
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
        }
}

