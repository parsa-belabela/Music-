package com.example.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
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
        AppTheme.PURE_LIQUID_GLASS -> Color(0xFF060812)
        AppTheme.CYBER_NIGHTS -> Color(0xFF030712)
        AppTheme.Y2K_CHROME -> Color(0xFF080B10)
        AppTheme.VELVET_NOIR -> Color(0xFF0B020E)
        AppTheme.SUNSET_RAVE -> Color(0xFF130314)
        AppTheme.DIGITAL_ACID -> Color(0xFF000000)
        AppTheme.MONOCHROME_NOIR -> Color(0xFF050508)
    },
    onBackground = TextPrimary,
    surface = when (appTheme) {
        AppTheme.PURE_LIQUID_GLASS -> Color(0x18FFFFFF)
        AppTheme.CYBER_NIGHTS -> Color(0xFF080E1C)
        AppTheme.Y2K_CHROME -> Color(0xFF101420)
        AppTheme.VELVET_NOIR -> Color(0xFF15041A)
        AppTheme.SUNSET_RAVE -> Color(0xFF1C061E)
        AppTheme.DIGITAL_ACID -> Color(0xFF050B05)
        AppTheme.MONOCHROME_NOIR -> Color(0x1AFFFFFF)
    },
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.PURE_LIQUID_GLASS,
    accentColor: Color = Color(0xFF00E5FF),
    content: @Composable () -> Unit
) {
    val colorScheme = createAuraColorScheme(appTheme, accentColor)
    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
