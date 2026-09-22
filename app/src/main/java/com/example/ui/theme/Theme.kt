package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = UberGreen,
    onPrimary = UberBlack,
    primaryContainer = UberGreenDark,
    onPrimaryContainer = UberWhite,
    secondary = UberWhite,
    onSecondary = UberBlack,
    secondaryContainer = UberDarkCard,
    onSecondaryContainer = UberWhite,
    tertiary = UberGreenLight,
    onTertiary = UberBlack,
    background = UberBlack,
    surface = UberDarkSurface,
    onBackground = UberWhite,
    onSurface = UberWhite,
    surfaceVariant = UberDarkCard,
    onSurfaceVariant = UberGray300,
    outline = UberDarkBorder,
    error = UberRed
)

private val LightColorScheme = lightColorScheme(
    primary = UberGreenDark,
    onPrimary = UberWhite,
    primaryContainer = UberGreenLight,
    onPrimaryContainer = UberBlack,
    secondary = UberBlack,
    onSecondary = UberWhite,
    secondaryContainer = UberGray100,
    onSecondaryContainer = UberBlack,
    tertiary = UberGreen,
    onTertiary = UberWhite,
    background = UberLightBackground,
    surface = UberWhite,
    onBackground = UberBlack,
    onSurface = UberBlack,
    surfaceVariant = UberLightBackground,
    onSurfaceVariant = UberGray700,
    outline = UberLightBorder,
    error = UberRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
