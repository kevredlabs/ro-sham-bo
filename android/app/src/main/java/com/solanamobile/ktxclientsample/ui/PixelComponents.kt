package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solanamobile.ktxclientsample.ui.theme.PixelBlack
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkGray
import com.solanamobile.ktxclientsample.ui.theme.PixelGray
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite

/**
 * A pixel-art styled button with a 3D drop-shadow effect.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bgColor: Color = PixelCyan,
    shadowColor: Color = PixelDarkBlue,
    textColor: Color = PixelBlack,
    buttonHeight: Dp = 48.dp
) {
    val actualBg = if (enabled) bgColor else PixelGray
    val actualShadow = if (enabled) shadowColor else PixelDarkGray
    val actualText = if (enabled) textColor else PixelWhite.copy(alpha = 0.5f)

    Box(modifier = modifier.height(buttonHeight + 4.dp)) {
        // Drop shadow (offset)
        Box(
            modifier = Modifier
                .offset(x = 4.dp, y = 4.dp)
                .fillMaxWidth()
                .height(buttonHeight)
                .background(actualShadow, RectangleShape)
        )
        // Main button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .background(actualBg, RectangleShape)
                .border(2.dp, PixelWhite, RectangleShape)
                .then(
                    if (enabled) Modifier.clickable(onClick = onClick)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.button,
                color = actualText
            )
        }
    }
}

/**
 * A pixel-art styled outlined button (border only, transparent background).
 */
@Composable
fun PixelOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = PixelCyan,
    textColor: Color = PixelCyan,
    buttonHeight: Dp = 48.dp
) {
    val actualBorder = if (enabled) borderColor else PixelGray
    val actualText = if (enabled) textColor else PixelGray

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .border(2.dp, actualBorder, RectangleShape)
            .background(Color.Transparent, RectangleShape)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.button,
            color = actualText
        )
    }
}

/**
 * CRT scanline overlay for a retro feel.
 * Place this as the last child inside a Box wrapping the screen.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineSpacing: Int = 4,
    alpha: Float = 0.08f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = Color.Black.copy(alpha = alpha)
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }
    }
}

/**
 * Wraps a screen with the pixel art background and scanline overlay.
 */
@Composable
fun PixelScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixelBlack)
    ) {
        content()
        ScanlineOverlay()
    }
}
