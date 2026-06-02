package app.spent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand accent: a single calm green, monochrome surfaces around it. Minimal by design.
private val Green = Color(0xFF16A34A)
private val GreenDark = Color(0xFF22C55E)
private val Spend = Color(0xFFDC2626) // expense amounts / over-budget

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5DF),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFF4B635A),
    onSecondary = Color.White,
    background = Color(0xFFFCFDFB),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFCFDFB),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFEEF1ED),
    onSurfaceVariant = Color(0xFF40493F),
    surfaceContainer = Color(0xFFF1F4EF),
    surfaceContainerHigh = Color(0xFFEBEEE8),
    surfaceContainerHighest = Color(0xFFE5E8E2),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFBFC9BF),
    error = Spend,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = GreenDark,
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF0B5128),
    onPrimaryContainer = Color(0xFFB5F2C5),
    secondary = Color(0xFFB2CCBF),
    onSecondary = Color(0xFF1E352B),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF101411),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF40493F),
    onSurfaceVariant = Color(0xFFBFC9BF),
    surfaceContainer = Color(0xFF1C201D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF313631),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF40493F),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005),
)

/** Expense amount color, theme-aware. */
val ColorScheme.spendColor: Color get() = error

@Composable
expect fun dynamicOrDefaultColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    lightScheme: ColorScheme,
    darkScheme: ColorScheme,
): ColorScheme

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = dynamicOrDefaultColorScheme(darkTheme, dynamicColor, LightColors, DarkColors)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography(),
        content = content,
    )
}
