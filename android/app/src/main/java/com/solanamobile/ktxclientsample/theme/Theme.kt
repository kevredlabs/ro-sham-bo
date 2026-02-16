package com.solanamobile.ktxclientsample.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val PixelColorPalette = darkColors(
    primary = PixelCyan,
    primaryVariant = PixelBlue,
    secondary = PixelYellow,
    secondaryVariant = PixelOrange,
    background = PixelBlack,
    surface = PixelDarkGray,
    error = PixelRed,
    onPrimary = PixelBlack,
    onSecondary = PixelBlack,
    onBackground = PixelWhite,
    onSurface = PixelWhite,
    onError = PixelWhite
)

@Composable
fun KtxClientSampleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = PixelColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
