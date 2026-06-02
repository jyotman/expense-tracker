package app.spent.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun dynamicOrDefaultColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    lightScheme: ColorScheme,
    darkScheme: ColorScheme,
): ColorScheme = if (darkTheme) darkScheme else lightScheme
