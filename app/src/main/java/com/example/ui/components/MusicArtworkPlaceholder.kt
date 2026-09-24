package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import kotlin.math.abs
import kotlin.math.min

/**
 * Curated harmonic and vivid themes for tracks that lack album artwork.
 * Prevents plain black darkness by generating an authentic, radiant visual identity.
 */
object GenerativeArtworkTheme {
    data class GradientTheme(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val deep: Color
    )

    val PRESETS = listOf(
        // Sunset Blaze
        GradientTheme(
            primary = Color(0xFFFF5252),
            secondary = Color(0xFFFF7A00),
            tertiary = Color(0xFFFFD600),
            deep = Color(0xFF1E0906)
        ),
        // Neon Ultraviolet
        GradientTheme(
            primary = Color(0xFF8E2DE2),
            secondary = Color(0xFF4A00E0),
            tertiary = Color(0xFFC471ED),
            deep = Color(0xFF120326)
        ),
        // Cyber Emerald & Mint
        GradientTheme(
            primary = Color(0xFF00E676),
            secondary = Color(0xFF00B0FF),
            tertiary = Color(0xFF1DE9B6),
            deep = Color(0xFF031A14)
        ),
        // Electric Coral & Magenta
        GradientTheme(
            primary = Color(0xFFFF2A6D),
            secondary = Color(0xFF8A2387),
            tertiary = Color(0xFFFF5E7E),
            deep = Color(0xFF1F0516)
        ),
        // Imperial Amber & Gold
        GradientTheme(
            primary = Color(0xFFFFB300),
            secondary = Color(0xFFFF6F00),
            tertiary = Color(0xFFFFE082),
            deep = Color(0xFF1F1202)
        ),
        // Sapphire Horizon
        GradientTheme(
            primary = Color(0xFF2979FF),
            secondary = Color(0xFF651FFF),
            tertiary = Color(0xFF00E5FF),
            deep = Color(0xFF060D24)
        ),
        // Velvet Orchid
        GradientTheme(
            primary = Color(0xFFD500F9),
            secondary = Color(0xFF651FFF),
            tertiary = Color(0xFFFF4081),
            deep = Color(0xFF1A021F)
        ),
        // Electric Lagoon
        GradientTheme(
            primary = Color(0xFF00E5FF),
            secondary = Color(0xFF00E676),
            tertiary = Color(0xFF76FF03),
            deep = Color(0xFF03191B)
        )
    )

    fun getThemeForTrack(title: String?, artist: String?, id: String? = null): GradientTheme {
        val seed = (title.orEmpty() + artist.orEmpty() + id.orEmpty()).hashCode().let {
            if (it == Int.MIN_VALUE) 0 else abs(it)
        }
        return PRESETS[seed % PRESETS.size]
    }
}

/**
 * Aesthetic, high-end cover artwork component displayed when a track has no artwork image.
 * Combines vibrant deterministic gradients, subtle vinyl groove rings, center emblem with
 * track letter monogram, and an authentic specular glass sheen.
 */
@Composable
fun MusicArtworkPlaceholder(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
    trackId: String? = null,
    palette: AmbientPalette? = null
) {
    val theme = remember(title, artist, trackId) {
        GenerativeArtworkTheme.getThemeForTrack(title, artist, trackId)
    }

    val primaryColor = palette?.primary ?: theme.primary
    val secondaryColor = theme.secondary
    val tertiaryColor = theme.tertiary
    val deepColor = theme.deep

    val monogramChar = remember(title) {
        title?.trim()?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor,
                        tertiaryColor.copy(alpha = 0.85f),
                        deepColor
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val minDim = min(maxWidth.value, maxHeight.value).dp

        // Concentric Vinyl / Harmonic Sound Groove rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val maxR = min(size.width, size.height) * 0.48f

            val ringSteps = 7
            for (i in 1..ringSteps) {
                val r = (maxR / ringSteps) * i
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f + (i % 2) * 0.04f),
                    radius = r,
                    center = centerOffset,
                    style = Stroke(width = 1.2f)
                )
            }

            // Outer edge vignette shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                    center = centerOffset,
                    radius = maxR * 1.05f
                ),
                radius = maxR * 1.05f,
                center = centerOffset
            )
        }

        // Center Vinyl Disc Label / Glass Monogram Badge
        val centerBadgeSize = minDim * 0.38f
        Box(
            modifier = Modifier
                .size(centerBadgeSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = primaryColor.copy(alpha = 0.70f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner delicate vinyl center ring
            Box(
                modifier = Modifier
                    .size(centerBadgeSize * 0.40f)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (monogramChar != null) {
                    Text(
                        text = monogramChar,
                        fontSize = (centerBadgeSize.value * 0.22f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((centerBadgeSize.value * 0.20f).dp)
                    )
                }
            }
        }

        // Specular Glass Sheen across the album sleeve (diagonal gloss highlight)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, size.height * 0.85f)
                )
            )
        }
    }
}
