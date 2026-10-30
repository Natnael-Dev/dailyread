package io.github.natnael_dev.dailyread.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BotanicalPrimary,
    onPrimary = BotanicalOnPrimary,
    primaryContainer = BotanicalPrimaryContainer,
    onPrimaryContainer = BotanicalOnPrimaryContainer,
    inversePrimary = BotanicalInversePrimary,
    secondary = BotanicalSecondary,
    onSecondary = BotanicalOnSecondary,
    secondaryContainer = BotanicalSecondaryContainer,
    onSecondaryContainer = BotanicalOnSecondaryContainer,
    tertiary = BotanicalTertiary,
    onTertiary = BotanicalOnTertiary,
    tertiaryContainer = BotanicalTertiaryContainer,
    onTertiaryContainer = BotanicalOnTertiaryContainer,
    background = BotanicalBackground,
    onBackground = BotanicalOnBackground,
    surface = BotanicalSurface,
    onSurface = BotanicalOnSurface,
    surfaceVariant = BotanicalSurfaceVariant,
    onSurfaceVariant = BotanicalOnSurfaceVariant,
    surfaceTint = BotanicalSurfaceTint,
    inverseSurface = BotanicalInverseSurface,
    inverseOnSurface = BotanicalInverseOnSurface,
    outline = BotanicalOutline,
    outlineVariant = BotanicalOutlineVariant,
    error = BotanicalError,
    onError = BotanicalOnError,
    errorContainer = BotanicalErrorContainer,
    onErrorContainer = BotanicalOnErrorContainer,
    surfaceContainerLowest = BotanicalSurfaceContainerLowest,
    surfaceContainerLow = BotanicalSurfaceContainerLow,
    surfaceContainer = BotanicalSurfaceContainer,
    surfaceContainerHigh = BotanicalSurfaceContainerHigh,
    surfaceContainerHighest = BotanicalSurfaceContainerHighest,
    surfaceDim = BotanicalSurfaceDim,
    surfaceBright = BotanicalSurfaceBright
)

private val DarkColorScheme = darkColorScheme(
    primary = BotanicalDarkPrimary,
    onPrimary = BotanicalDarkOnPrimary,
    primaryContainer = BotanicalDarkPrimaryContainer,
    onPrimaryContainer = BotanicalDarkOnPrimaryContainer,
    secondary = BotanicalDarkSecondary,
    onSecondary = BotanicalDarkOnSecondary,
    secondaryContainer = BotanicalDarkSecondaryContainer,
    onSecondaryContainer = BotanicalDarkOnSecondaryContainer,
    tertiary = BotanicalDarkTertiary,
    onTertiary = BotanicalDarkOnTertiary,
    tertiaryContainer = BotanicalDarkTertiaryContainer,
    onTertiaryContainer = BotanicalDarkOnTertiaryContainer,
    background = BotanicalDarkBackground,
    onBackground = BotanicalDarkOnBackground,
    surface = BotanicalDarkSurface,
    onSurface = BotanicalDarkOnSurface,
    surfaceVariant = BotanicalDarkSurfaceVariant,
    onSurfaceVariant = BotanicalDarkOnSurfaceVariant,
    outline = BotanicalDarkOutline,
    outlineVariant = BotanicalDarkOutlineVariant,
    error = BotanicalDarkError,
    onError = BotanicalDarkOnError,
    errorContainer = BotanicalDarkErrorContainer,
    onErrorContainer = BotanicalDarkOnErrorContainer,
    surfaceContainerLowest = BotanicalDarkSurfaceContainerLowest,
    surfaceContainerLow = BotanicalDarkSurfaceContainerLow,
    surfaceContainer = BotanicalDarkSurfaceContainer,
    surfaceContainerHigh = BotanicalDarkSurfaceContainerHigh,
    surfaceContainerHighest = BotanicalDarkSurfaceContainerHighest
)

@Composable
fun DailyReadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BotanicalTypography,
        shapes = BotanicalShapes,
        content = content
    )
}
