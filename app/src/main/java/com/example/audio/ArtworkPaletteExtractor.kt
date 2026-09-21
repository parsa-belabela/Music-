package com.example.audio

import androidx.compose.ui.graphics.Color
import com.example.data.model.Track
import kotlin.math.abs

data class AmbientPalette(
    val primary: Color,
    val secondary: Color,
    val haloGlow: Color,
    val accent: Color,
    val deepAtmosphere: Color = Color(0xFF07070E),
    val isLightLuminance: Boolean = false
)

object ArtworkPaletteExtractor {
    fun extract(track: Track?, fallbackAccent: Color): AmbientPalette {
        if (track == null) {
            return AmbientPalette(
                primary = fallbackAccent,
                secondary = fallbackAccent.copy(alpha = 0.55f),
                haloGlow = fallbackAccent.copy(alpha = 0.35f),
                accent = fallbackAccent,
                deepAtmosphere = Color(0xFF080812),
                isLightLuminance = false
            )
        }

        // Generate rich, premium, cinematic color pairings inspired by album mood
        val seed = abs((track.title + track.artist + track.genre + track.album).hashCode())
        val hueCategory = seed % 9

        val (c1, c2, c3, deep) = when (hueCategory) {
            0 -> Quad(Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFF0E0B1F)) // Purple / Electric Cyan / Deep Twilight
            1 -> Quad(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFF94B8), Color(0xFF1F0B13)) // Crimson Velvet / Sunset Rose
            2 -> Quad(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF67E8F9), Color(0xFF05121F)) // Cyan Ocean / Electric Blue
            3 -> Quad(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF34D399), Color(0xFF061A14)) // Emerald Forest / Bright Teal
            4 -> Quad(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFF1C0F05)) // Liquid Amber / Warm Gold
            5 -> Quad(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFF818CF8), Color(0xFF0C0B1E)) // Indigo Aura / Royal Violet
            6 -> Quad(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFF472B6), Color(0xFF1A0918)) // Magenta / Neon Ultraviolet
            7 -> Quad(Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFF5EEAD4), Color(0xFF081717)) // Deep Teal / Cosmic Blue
            else -> Quad(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF60A5FA), Color(0xFF090E1D)) // Cobalt / Stellar Indigo
        }

        return AmbientPalette(
            primary = c1,
            secondary = c2,
            haloGlow = c1.copy(alpha = 0.42f),
            accent = c3,
            deepAtmosphere = deep,
            isLightLuminance = false
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
