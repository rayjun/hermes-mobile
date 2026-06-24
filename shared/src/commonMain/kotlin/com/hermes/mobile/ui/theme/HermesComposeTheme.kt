package com.hermes.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class HermesColorScheme(
    val background: Long,
    val backgroundSubtle: Long,
    val surface: Long,
    val surfaceMuted: Long,
    val border: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val textTertiary: Long,
    val accent: Long,
    val error: Long,
    val success: Long,
    val warning: Long,
    val isLight: Boolean,
)

object HermesThemeTokens {
    fun light(): HermesColorScheme = HermesColorScheme(
        background = HermesColors.Background,
        backgroundSubtle = HermesColors.BackgroundSubtle,
        surface = HermesColors.Surface,
        surfaceMuted = HermesColors.SurfaceMuted,
        border = HermesColors.Border,
        textPrimary = HermesColors.TextPrimary,
        textSecondary = HermesColors.TextSecondary,
        textTertiary = HermesColors.TextTertiary,
        accent = HermesColors.Blue,
        error = HermesColors.Error,
        success = HermesColors.Success,
        warning = HermesColors.Warning,
        isLight = true,
    )
}

val LocalHermesColors = staticCompositionLocalOf { HermesThemeTokens.light() }

object HermesTheme {
    val colors: HermesColorScheme
        @Composable get() = LocalHermesColors.current
}

@Composable
fun HermesTheme(
    colors: HermesColorScheme = HermesThemeTokens.light(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHermesColors provides colors,
        content = content,
    )
}
