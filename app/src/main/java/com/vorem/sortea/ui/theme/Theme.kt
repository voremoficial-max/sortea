package com.vorem.sortea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SorteaColors = darkColorScheme(
    primary = SorteaGold,
    onPrimary = SorteaBlack,
    secondary = SorteaGoldDark,
    onSecondary = SorteaWhite,
    tertiary = SorteaGrayLight,
    onTertiary = SorteaBlack,
    background = SorteaBlack,
    onBackground = SorteaWhite,
    surface = SorteaBlackSoft,
    onSurface = SorteaWhite,
    surfaceVariant = SorteaGray,
    onSurfaceVariant = SorteaGrayLight
)

@Composable
fun SorteaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SorteaColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
