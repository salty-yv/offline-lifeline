package com.example.offlinelifeline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = LifelineAccent,
    onPrimary = Color.White,
    primaryContainer = LifelineAccent,
    onPrimaryContainer = Color.White,
    secondary = LifelineForegroundSecondary,
    tertiary = LifelineSuccess,
    background = LifelineSurfacePrimary,
    onBackground = LifelineForegroundPrimary,
    surface = LifelineSurfacePrimary,
    onSurface = LifelineForegroundPrimary,
    surfaceVariant = LifelineSurfaceSecondary,
    onSurfaceVariant = LifelineForegroundSecondary,
    surfaceContainer = LifelineSurfaceSecondary,
    surfaceContainerHighest = LifelineBorderPrimary,
    outline = LifelineBorderPrimary,
    error = LifelineDanger,
    errorContainer = LifelineDangerSoft,
    onErrorContainer = LifelineDanger
)

private val LightColorScheme = lightColorScheme(
    primary = LifelineAccent,
    onPrimary = Color.White,
    primaryContainer = LifelineAccent,
    onPrimaryContainer = Color.White,
    secondary = LifelineForegroundSecondary,
    onSecondary = Color.White,
    tertiary = LifelineSuccess,
    background = LifelineSurfacePrimary,
    onBackground = LifelineForegroundPrimary,
    surface = LifelineSurfacePrimary,
    onSurface = LifelineForegroundPrimary,
    surfaceVariant = LifelineSurfaceSecondary,
    onSurfaceVariant = LifelineForegroundSecondary,
    surfaceContainer = LifelineSurfaceSecondary,
    surfaceContainerHighest = LifelineBorderPrimary,
    outline = LifelineBorderPrimary,
    outlineVariant = LifelineBorderPrimary,
    error = LifelineDanger,
    errorContainer = LifelineDangerSoft,
    onErrorContainer = LifelineDanger
)

private val LifelineShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun OfflineLifelineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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
        typography = Typography,
        shapes = LifelineShapes,
        content = content
    )
}
