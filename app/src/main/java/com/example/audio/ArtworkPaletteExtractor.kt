package com.example.audio

import androidx.compose.ui.graphics.Color
import com.example.data.model.Track
import kotlin.math.abs

data class AmbientPalette(
    val primary: Color,
    val secondary: Color,
    val haloGlow: Color,
    val accent: Color
)

object ArtworkPaletteExtractor {
    fun extract(track: Track?, fallbackAccent: Color): AmbientPalette {
        if (track == null) {
            return AmbientPalette(
                primary = fallbackAccent,
                secondary = fallbackAccent.copy(alpha = 0.5f),
                haloGlow = fallbackAccent.copy(alpha = 0.25f),
                accent = fallbackAccent
            )
        }

        // Generate distinctive chromatic ambient schemes based on track metadata or genre
        val seed = abs((track.title + track.artist + track.genre).hashCode())
        val hueCategory = seed % 8

        val (c1, c2, c3) = when (hueCategory) {
            0 -> Triple(Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFC084FC)) // Purple / Cyan
            1 -> Triple(Color(0xFFEC4899), Color(0xFFF43F5E), Color(0xFFFB7185)) // Neon Pink / Rose
            2 -> Triple(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF67E8F9)) // Cyan / Deep Blue
            3 -> Triple(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF34D399)) // Emerald / Teal
            4 -> Triple(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFFBBF24)) // Amber / Crimson
            5 -> Triple(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFF818CF8)) // Indigo / Violet
            6 -> Triple(Color(0xFFD946EF), Color(0xFF8B5CF6), Color(0xFFF472B6)) // Fuchsia / Purple
            else -> Triple(Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFF5EEAD4)) // Teal / Indigo
        }

        return AmbientPalette(
            primary = c1,
            secondary = c2,
            haloGlow = c1.copy(alpha = 0.35f),
            accent = c3
        )
    }
}
