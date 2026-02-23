package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solanamobile.ktxclientsample.ui.theme.PixelBlack
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkGray
import com.solanamobile.ktxclientsample.ui.theme.PixelLightBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelOrange
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

private fun DrawScope.drawPixel(col: Int, row: Int, color: Color, pixelSize: Float) {
    drawRect(
        color = color,
        topLeft = Offset(col * pixelSize, row * pixelSize),
        size = Size(pixelSize, pixelSize)
    )
}

@Composable
fun PixelRockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val px = size.width / 12f
        val main = Color(0xFF94b0c2)
        val shadow = Color(0xFF566c86)
        val highlight = Color(0xFFc0d0da)

        // Row 2
        for (c in 4..7) drawPixel(c, 2, main, px)
        // Row 3
        for (c in 3..8) drawPixel(c, 3, main, px)
        drawPixel(4, 3, highlight, px)
        drawPixel(5, 3, highlight, px)
        // Row 4
        for (c in 2..9) drawPixel(c, 4, main, px)
        drawPixel(3, 4, highlight, px)
        // Row 5
        for (c in 2..9) drawPixel(c, 5, main, px)
        // Row 6
        for (c in 2..9) drawPixel(c, 6, main, px)
        drawPixel(8, 6, shadow, px)
        drawPixel(9, 6, shadow, px)
        // Row 7
        for (c in 3..8) drawPixel(c, 7, main, px)
        drawPixel(7, 7, shadow, px)
        drawPixel(8, 7, shadow, px)
        // Row 8
        for (c in 3..8) drawPixel(c, 8, shadow, px)
        // Row 9
        for (c in 4..7) drawPixel(c, 9, shadow, px)
    }
}

@Composable
fun PixelPaperIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val px = size.width / 12f
        val main = PixelWhite
        val shadow = Color(0xFFc0c0c0)
        val line = PixelLightBlue
        val fold = PixelCyan

        // Sheet outline rows 1-10
        for (r in 1..9) {
            for (c in 3..8) drawPixel(c, r, main, px)
        }
        // Right shadow edge
        for (r in 2..9) drawPixel(9, r, shadow, px)
        // Bottom shadow
        for (c in 4..9) drawPixel(c, 10, shadow, px)

        // Folded corner (top-right)
        drawPixel(8, 1, fold, px)
        drawPixel(9, 1, fold, px)
        drawPixel(9, 2, fold, px)

        // Text lines
        for (c in 4..7) drawPixel(c, 3, line, px)
        for (c in 4..6) drawPixel(c, 5, line, px)
        for (c in 4..7) drawPixel(c, 7, line, px)
    }
}

@Composable
fun PixelScissorsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val px = size.width / 12f
        val blade = PixelCyan
        val shadow = PixelTeal
        val handle = PixelOrange
        val handleShadow = Color(0xFFc05030)

        // Left blade (top-left to center)
        drawPixel(2, 2, blade, px)
        drawPixel(3, 2, blade, px)
        drawPixel(3, 3, blade, px)
        drawPixel(4, 3, blade, px)
        drawPixel(4, 4, blade, px)
        drawPixel(5, 4, blade, px)
        drawPixel(5, 5, blade, px)
        drawPixel(6, 5, blade, px)

        // Right blade (top-right to center)
        drawPixel(9, 2, blade, px)
        drawPixel(8, 2, blade, px)
        drawPixel(8, 3, blade, px)
        drawPixel(7, 3, blade, px)
        drawPixel(7, 4, blade, px)
        drawPixel(6, 4, blade, px)
        drawPixel(6, 5, blade, px)
        drawPixel(5, 5, shadow, px)

        // Blade shadows
        drawPixel(2, 3, shadow, px)
        drawPixel(9, 3, shadow, px)

        // Center pivot
        drawPixel(5, 6, PixelYellow, px)
        drawPixel(6, 6, PixelYellow, px)

        // Left handle ring
        drawPixel(4, 7, handle, px)
        drawPixel(3, 7, handle, px)
        drawPixel(3, 8, handle, px)
        drawPixel(3, 9, handle, px)
        drawPixel(4, 9, handle, px)
        drawPixel(5, 8, handle, px)
        drawPixel(5, 7, handle, px)
        drawPixel(4, 8, PixelBlack, px)

        // Right handle ring
        drawPixel(7, 7, handle, px)
        drawPixel(8, 7, handle, px)
        drawPixel(8, 8, handle, px)
        drawPixel(8, 9, handle, px)
        drawPixel(7, 9, handle, px)
        drawPixel(6, 8, handle, px)
        drawPixel(6, 7, handle, px)
        drawPixel(7, 8, PixelBlack, px)

        // Handle shadows
        drawPixel(3, 9, handleShadow, px)
        drawPixel(4, 9, handleShadow, px)
        drawPixel(7, 9, handleShadow, px)
        drawPixel(8, 9, handleShadow, px)
    }
}

@Composable
fun PixelMoveCard(
    label: String,
    borderColor: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shadowOffset: Dp = 4.dp,
    icon: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Shadow
        Box(
            modifier = Modifier
                .offset(x = shadowOffset, y = shadowOffset)
                .fillMaxSize()
                .background(shadowColor, RectangleShape)
        )
        // Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelDarkGray, RectangleShape)
                .border(3.dp, borderColor, RectangleShape)
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.button,
                color = borderColor
            )
        }
    }
}
