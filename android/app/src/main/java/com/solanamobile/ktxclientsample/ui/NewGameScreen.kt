package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun NewGameScreen(
    pin: String,
    isLoading: Boolean,
    error: String,
    onCancel: () -> Unit
) {
    val titleStyle = MaterialTheme.typography.h3.copy(
        fontSize = 24.sp,
        letterSpacing = 3.sp
    )

    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PixelShadowText(
                    text = "WAITING FOR",
                    color = PixelCyan,
                    shadowColor = PixelTeal,
                    style = titleStyle
                )
                PixelShadowText(
                    text = "PLAYER 2...",
                    color = PixelCyan,
                    shadowColor = PixelTeal,
                    style = titleStyle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Share this PIN",
                style = MaterialTheme.typography.h5,
                color = PixelLightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = pin,
                style = MaterialTheme.typography.h3.copy(
                    fontSize = 40.sp,
                    letterSpacing = 12.sp
                ),
                color = PixelYellow,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .border(3.dp, PixelYellow, RectangleShape)
                    .padding(horizontal = 32.dp, vertical = 20.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.body1,
                    color = PixelYellow,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            PixelButton(
                text = if (isLoading) "Cancelling..." else "Cancel the Game",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                bgColor = PixelCyan,
                buttonHeight = 64.dp,
                textStyle = MaterialTheme.typography.h5,
                borderWidth = 3.dp,
                shadowOffset = 5.dp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
