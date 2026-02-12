package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Main menu shown after wallet connection.
 * New Game / Join Game actions and wallet info (pubkey, balance, network) at the bottom.
 */
@Composable
fun MainMenuScreen(
    userAddress: String,
    solBalance: Double,
    network: String,
    onNewGame: () -> Unit = {},
    onJoinGame: () -> Unit = {},
    onDisconnect: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onNewGame
            ) {
                Text("New Game")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onJoinGame
            ) {
                Text("Join Game")
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = userAddress.ifEmpty { "—" },
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Balance: ${if (solBalance >= 0) "$solBalance SOL" else "—"}",
                    style = MaterialTheme.typography.body2
                )
                Text(
                    text = "Network: $network",
                    style = MaterialTheme.typography.body2
                )
            }

            onDisconnect?.let { disconnect ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = disconnect
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}
