package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

fun createAuraColorScheme(appTheme: AppTheme, accentColor: Color = RadiantPurple) = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = accentColor.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = ElectricBlue.copy(alpha = 0.2f),
    tertiary = NeonRose,
    background = when (appTheme) {
        AppTheme.GLASS -> GlassBackground
        AppTheme.LEGO -> Color(0xFF101115)
        AppTheme.CARTOON -> Color(0xFF13091B)
        AppTheme.CYBER_CHROME -> Color(0xFF040711)
        AppTheme.VAPORWAVE -> Color(0xFF110820)
        AppTheme.OBSIDIAN_MATRIX -> Color(0xFF020703)
    },
    onBackground = TextPrimary,
    surface = when (appTheme) {
        AppTheme.OBSIDIAN_MATRIX -> Color(0xFF030E06)
        AppTheme.LEGO -> Color(0xFF1B1D23)
        AppTheme.CARTOON -> Color(0xFF1B0F24)
        AppTheme.CYBER_CHROME -> Color(0xFF080D1A)
        AppTheme.VAPORWAVE -> Color(0xFF190C2C)
        AppTheme.GLASS -> Color(0xFF0E1320)
    },
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.GLASS,
    accentColor: Color = RadiantPurple,
    content: @Composable () -> Unit
) {
    val colorScheme = createAuraColorScheme(appTheme, accentColor)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
