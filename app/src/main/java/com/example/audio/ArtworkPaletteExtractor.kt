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
            AppTheme.GLASS -> AmbientPalette(
                primary = Color(0xFF06B6D4),
                secondary = Color(0xFF3B82F6),
                haloGlow = Color(0x6606B6D4),
                accent = Color(0xFF67E8F9),
                deepAtmosphere = Color(0xFF050B16)
            )
            AppTheme.LEGO -> AmbientPalette(
                primary = Color(0xFFFF2D20),
                secondary = Color(0xFFFFD600),
                haloGlow = Color(0x66FF2D20),
                accent = Color(0xFF00E5FF),
                deepAtmosphere = Color(0xFF180A0A)
            )
            AppTheme.CARTOON -> AmbientPalette(
                primary = Color(0xFFFF4081),
                secondary = Color(0xFF00E5FF),
                haloGlow = Color(0x66FF4081),
                accent = Color(0xFFFFD600),
                deepAtmosphere = Color(0xFF140718)
            )
            AppTheme.CYBER_CHROME -> AmbientPalette(
                primary = Color(0xFF00F0FF),
                secondary = Color(0xFFFF0055),
                haloGlow = Color(0x7700F0FF),
                accent = Color(0xFF7000FF),
                deepAtmosphere = Color(0xFF060913)
            )
            AppTheme.VAPORWAVE -> AmbientPalette(
                primary = Color(0xFFFF71CE),
                secondary = Color(0xFF01CDFE),
                haloGlow = Color(0x66FF71CE),
                accent = Color(0xFF05FFA1),
                deepAtmosphere = Color(0xFF130826)
            )
            AppTheme.OBSIDIAN_MATRIX -> AmbientPalette(
                primary = Color(0xFF00FF66),
                secondary = Color(0xFF00CC44),
                haloGlow = Color(0x6600FF66),
                accent = Color(0xFF39FF14),
                deepAtmosphere = Color(0xFF020904)
            )
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
