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
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun NewGameScreen(
    pin: String,
    onBack: () -> Unit
) {
    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WAITING FOR\nPLAYER 2...",
                style = MaterialTheme.typography.h6,
                color = PixelCyan,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Share this PIN",
                style = MaterialTheme.typography.body1,
                color = PixelLightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = pin,
                style = MaterialTheme.typography.h3.copy(
                    fontSize = 32.sp,
                    letterSpacing = 8.sp
                ),
                color = PixelYellow,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .border(2.dp, PixelYellow, RectangleShape)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
            PixelOutlinedButton(
                text = "Back to Menu",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
