package com.example.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = PureWhite,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Teal600,
    onSecondary = PureWhite,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal700,
    tertiary = Amber600,
    onTertiary = PureWhite,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Charcoal900,
    background = OffWhiteBg,
    onBackground = Charcoal900,
    surface = PureWhite,
    onSurface = Charcoal900,
    surfaceVariant = Charcoal100,
    onSurfaceVariant = Charcoal700,
    outline = Charcoal200,
    error = Red600,
    onError = PureWhite,
    errorContainer = Red100,
    onErrorContainer = Charcoal900
)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Charcoal950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,
    secondary = Teal500,
    onSecondary = Charcoal950,
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal50,
    tertiary = Amber500,
    onTertiary = Charcoal950,
    tertiaryContainer = Charcoal800,
    onTertiaryContainer = Amber100,
    background = DarkBackground,
    onBackground = OffWhiteBg,
    surface = DarkSurface,
    onSurface = OffWhiteBg,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Charcoal400,
    outline = DarkBorder,
    error = Red500,
    onError = Charcoal950,
    errorContainer = Charcoal800,
    onErrorContainer = Red100
)

@Composable
fun ElajxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional brand colors by default per design guidelines
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElajxTypography,
        shapes = ElajxShapes,
        content = content
    )
}
