package com.h2o.store.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define Color objects that reference the XML color resources
val H2OBlue = Color(0xFF0288D1)
val H2OLightBlue = Color(0xFF03A9F4)
val H2ODeepBlue = Color(0xFF01579B)
val H2OTeal = Color(0xFF80DEEA)
val H2OSkyBlue = Color(0xFFBBDEFB)
val ErrorRed = Color(0xFFE53935)
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFC107)
val TextPrimary = Color(0xFF263238)
val TextSecondary = Color(0xFF607D8B)
val BackgroundLight = Color(0xFFF5F5F5)
val DividerColor = Color(0xFFE0E0E0)

private val LightColorScheme = lightColorScheme(
    primary = H2OBlue,
    onPrimary = Color.White,
    primaryContainer = H2OSkyBlue,
    onPrimaryContainer = H2ODeepBlue,
    secondary = H2OLightBlue,
    onSecondary = Color.White,
    secondaryContainer = H2OTeal,
    onSecondaryContainer = H2ODeepBlue,
    tertiary = H2OTeal,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = H2OLightBlue,
    onPrimary = Color.Black,
    primaryContainer = H2ODeepBlue,
    onPrimaryContainer = H2OSkyBlue,
    secondary = H2OTeal,
    onSecondary = Color.Black,
    secondaryContainer = H2ODeepBlue,
    onSecondaryContainer = H2OTeal,
    tertiary = H2OTeal,
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.Black
)

@Composable
fun H2OTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}