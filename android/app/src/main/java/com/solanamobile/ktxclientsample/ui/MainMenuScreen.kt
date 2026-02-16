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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelGreen
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelOrange
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun MainMenuScreen(
    userAddress: String,
    solBalance: Double,
    network: String,
    onNewGame: () -> Unit = {},
    onJoinGame: () -> Unit = {},
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

            Text(
                text = "SEEKER RPS",
                style = MaterialTheme.typography.h3,
                color = PixelCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            PixelButton(
                text = "New Game",
                onClick = onNewGame,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            PixelButton(
                text = "Join Game",
                onClick = onJoinGame,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelOrange
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PixelLightGray.copy(alpha = 0.3f), RectangleShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "WALLET",
                    style = MaterialTheme.typography.caption,
                    color = PixelLightGray
                )
                Text(
                    text = userAddress.ifEmpty { "-" },
                    style = MaterialTheme.typography.body2,
                    color = PixelYellow,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "BAL: ${if (solBalance >= 0) "$solBalance SOL" else "-"}",
                    style = MaterialTheme.typography.body2,
                    color = PixelCyan
                )
                Text(
                    text = "NET: $network",
                    style = MaterialTheme.typography.body2,
                    color = PixelLightGray
                )
            }

            onDisconnect?.let { disconnect ->
                Spacer(modifier = Modifier.height(12.dp))
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
