package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

fun createAuraColorScheme(appTheme: AppTheme, accentColor: Color = AuraSpotifyGreen) = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.Black,
    primaryContainer = AuraSurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = AuraSecondaryAccent,
    onSecondary = Color.Black,
    secondaryContainer = AuraSurfaceHighlight,
    tertiary = AuraSpotifyGreen,
    background = when (appTheme) {
        AppTheme.DIGITAL_ACID -> Color(0xFF000000)
        AppTheme.MONOCHROME_NOIR -> Color(0xFF0A0A0A)
        else -> AuraCanvas
    },
    onBackground = TextPrimary,
    surface = when (appTheme) {
        AppTheme.DIGITAL_ACID -> Color(0xFF0E0E0E)
        AppTheme.MONOCHROME_NOIR -> Color(0xFF141414)
        else -> AuraSurface
    },
    onSurface = TextPrimary,
    surfaceVariant = AuraSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = AuraBorderSubtle
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.PURE_LIQUID_GLASS,
    accentColor: Color = AuraSpotifyGreen,
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

