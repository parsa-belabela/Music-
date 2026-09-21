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
        AppTheme.MIDNIGHT -> MidnightBackground
        AppTheme.PURE_BLACK, AppTheme.AMOLED -> PureBlackBackground
        AppTheme.GRAPHITE -> GraphiteBackground
        AppTheme.GLASS -> GlassBackground
        AppTheme.NEON -> NeonBackground
        AppTheme.CINEMA -> CinemaBackground
        AppTheme.MINIMAL -> MinimalBackground
    },
    onBackground = TextPrimary,
    surface = when (appTheme) {
        AppTheme.PURE_BLACK, AppTheme.AMOLED -> Color(0xFF080808)
        AppTheme.GRAPHITE -> Color(0xFF181A1F)
        AppTheme.GLASS -> Color(0xFF0E1320)
        else -> SurfaceDark
    },
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.MIDNIGHT,
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
