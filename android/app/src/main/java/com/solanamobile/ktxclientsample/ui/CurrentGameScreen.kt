package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Current game screen: countdown 3-2-1, then Rock/Paper/Scissors selection,
 * then wait for other player, then result countdown 3-2-1, then result message.
 */
@Composable
fun CurrentGameScreen(
    gameId: String,
    gamePhase: String?,
    countdownNumber: Int,
    gameResultMessage: String?,
    error: String,
    onScreenVisible: (String) -> Unit,
    onSubmitChoice: (String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(gameId) {
        onScreenVisible(gameId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back to menu")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (gamePhase) {
                "COUNTDOWN", "RESULT_COUNTDOWN" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownNumber.toString(),
                            style = MaterialTheme.typography.h3.copy(
                                fontSize = 72.sp
                            ),
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
                "SELECTION" -> {
                    Text(
                        text = "Choose your move",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error.isNotEmpty()) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf("Rock", "Paper", "Scissors").forEach { choice ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                onClick = { onSubmitChoice(choice) }
                            ) {
                                Text(choice)
                            }
                        }
                    }
                }
                "WAITING_FOR_OTHER" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for other player...",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                "RESULT" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gameResultMessage ?: "—",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Getting ready...",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
