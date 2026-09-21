package com.example.audio

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppSettings
import com.example.data.model.AppTheme
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

    fun extract(
        track: Track?,
        appSettings: AppSettings
    ): AmbientPalette {
        val theme = appSettings.theme
        val customAccent = Color(appSettings.customAccentColor)

        // If track is available and auto-color is enabled, extract dynamic palette
        if (track != null && appSettings.autoColorFromArtwork) {
            val seed = abs((track.title + track.artist + track.genre + track.album).hashCode())
            val hueCategory = seed % 8

            val (c1, c2, c3, deep) = when (hueCategory) {
                0 -> Quad(Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFF0E0B1F)) // Purple / Cyan
                1 -> Quad(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFF94B8), Color(0xFF1F0B13)) // Crimson / Rose
                2 -> Quad(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF67E8F9), Color(0xFF05121F)) // Cyan / Blue
                3 -> Quad(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF34D399), Color(0xFF061A14)) // Emerald / Teal
                4 -> Quad(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFF1C0F05)) // Amber / Gold
                5 -> Quad(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFF818CF8), Color(0xFF0C0B1E)) // Indigo / Violet
                6 -> Quad(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFF472B6), Color(0xFF1A0918)) // Magenta / Purple
                else -> Quad(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF60A5FA), Color(0xFF090E1D)) // Blue / Indigo
            }

            return AmbientPalette(
                primary = c1,
                secondary = c2,
                haloGlow = c1.copy(alpha = 0.45f),
                accent = c3,
                deepAtmosphere = deep,
                isLightLuminance = false
            )
        }

        // Theme-driven dynamic chromatic palette
        return when (theme) {
            AppTheme.MIDNIGHT -> AmbientPalette(
                primary = Color(0xFF8B5CF6),
                secondary = Color(0xFF38BDF8),
                haloGlow = Color(0x668B5CF6),
                accent = Color(0xFFA78BFA),
                deepAtmosphere = Color(0xFF070814)
            )
            AppTheme.PURE_BLACK -> AmbientPalette(
                primary = Color(0xFF6366F1),
                secondary = Color(0xFF818CF8),
                haloGlow = Color(0x556366F1),
                accent = Color(0xFFA5B4FC),
                deepAtmosphere = Color(0xFF000000)
            )
            AppTheme.AMOLED -> AmbientPalette(
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFF818CF8),
                haloGlow = Color(0x5538BDF8),
                accent = Color(0xFF7DD3FC),
                deepAtmosphere = Color(0xFF000000)
            )
            AppTheme.GRAPHITE -> AmbientPalette(
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFF94A3B8),
                haloGlow = Color(0x5538BDF8),
                accent = Color(0xFF7DD3FC),
                deepAtmosphere = Color(0xFF101216)
            )
            AppTheme.GLASS -> AmbientPalette(
                primary = Color(0xFF06B6D4),
                secondary = Color(0xFF3B82F6),
                haloGlow = Color(0x6606B6D4),
                accent = Color(0xFF67E8F9),
                deepAtmosphere = Color(0xFF050B16)
            )
            AppTheme.NEON -> AmbientPalette(
                primary = Color(0xFFF43F5E),
                secondary = Color(0xFF06B6D4),
                haloGlow = Color(0x77F43F5E),
                accent = Color(0xFFFB7185),
                deepAtmosphere = Color(0xFF080312)
            )
            AppTheme.CINEMA -> AmbientPalette(
                primary = Color(0xFFF59E0B),
                secondary = Color(0xFFEA580C),
                haloGlow = Color(0x66F59E0B),
                accent = Color(0xFFFBBF24),
                deepAtmosphere = Color(0xFF0D0907)
            )
            AppTheme.MINIMAL -> AmbientPalette(
                primary = Color(0xFFE2E8F0),
                secondary = Color(0xFF94A3B8),
                haloGlow = Color(0x44E2E8F0),
                accent = Color(0xFFF8FAFC),
                deepAtmosphere = Color(0xFF0B0B0E)
            )
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
