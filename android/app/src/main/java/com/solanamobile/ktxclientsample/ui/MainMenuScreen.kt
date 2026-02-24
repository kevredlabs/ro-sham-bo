package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun MainMenuScreen(
    userAddress: String,
    solBalance: Double,
    network: String,
    onNewGame: () -> Unit = {},
    onJoinGame: () -> Unit = {},
    onRules: () -> Unit = {},
    onDisconnect: (() -> Unit)? = null
) {
    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            val titleStyle = MaterialTheme.typography.h3.copy(
                fontSize = 32.sp,
                letterSpacing = 3.sp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PixelShadowText(
                    text = "ROCK",
                    color = PixelCyan,
                    shadowColor = PixelTeal,
                    style = titleStyle
                )
                PixelShadowText(
                    text = "PAPER",
                    color = PixelLightBlue,
                    shadowColor = PixelDarkBlue,
                    style = titleStyle
                )
                PixelShadowText(
                    text = "SCISSORS",
                    color = PixelCyan,
                    shadowColor = PixelTeal,
                    style = titleStyle
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PixelButton(
                text = "New Game",
                onClick = onNewGame,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelCyan,
                buttonHeight = 64.dp,
                textStyle = MaterialTheme.typography.h5,
                borderWidth = 3.dp,
                shadowOffset = 5.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            PixelButton(
                text = "Join Game",
                onClick = onJoinGame,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelLightBlue,
                shadowColor = PixelDarkBlue,
                buttonHeight = 64.dp,
                textStyle = MaterialTheme.typography.h5,
                borderWidth = 3.dp,
                shadowOffset = 5.dp
            )

            Spacer(modifier = Modifier.weight(1f))

            PixelOutlinedButton(
                text = "Rules",
                onClick = onRules,
                modifier = Modifier.fillMaxWidth(),
                borderColor = PixelYellow,
                textColor = PixelYellow,
                buttonHeight = 40.dp
            )

            Spacer(modifier = Modifier.height(15.dp))

            onDisconnect?.let { disconnect ->
                PixelOutlinedButton(
                    text = "Disconnect",
                    onClick = disconnect,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = PixelLightGray,
                    textColor = PixelLightGray,
                    buttonHeight = 40.dp
                )
            }
        }
    }
}
