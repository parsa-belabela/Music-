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
        AppTheme.CYBER_NIGHTS -> Color(0xFF070B14)
        AppTheme.Y2K_CHROME -> Color(0xFF080B10)
        AppTheme.VELVET_NOIR -> Color(0xFF0B030D)
        AppTheme.SUNSET_RAVE -> Color(0xFF100512)
        AppTheme.DIGITAL_ACID -> Color(0xFF030503)
        AppTheme.MINIMAL_STUDIO -> Color(0xFF0C0C0F)
    },
    onBackground = TextPrimary,
    surface = when (appTheme) {
        AppTheme.CYBER_NIGHTS -> Color(0xFF0E172A)
        AppTheme.Y2K_CHROME -> Color(0xFF131824)
        AppTheme.VELVET_NOIR -> Color(0xFF18071E)
        AppTheme.SUNSET_RAVE -> Color(0xFF220A24)
        AppTheme.DIGITAL_ACID -> Color(0xFF070E06)
        AppTheme.MINIMAL_STUDIO -> Color(0xFF16161C)
    },
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.CYBER_NIGHTS,
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
