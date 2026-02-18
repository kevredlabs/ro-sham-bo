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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelGreen
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelOrange
import com.solanamobile.ktxclientsample.ui.theme.PixelRed
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

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

    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            PixelOutlinedButton(
                text = "< Menu",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.35f),
                buttonHeight = 36.dp,
                borderColor = PixelLightGray,
                textColor = PixelLightGray
            )

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
                                fontSize = 96.sp,
                                letterSpacing = 4.sp
                            ),
                            color = PixelYellow
                        )
                    }
                }
                "SELECTION" -> {
                    Text(
                        text = "CHOOSE YOUR MOVE",
                        style = MaterialTheme.typography.h6,
                        color = PixelCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error.isNotEmpty()) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.body2,
                            color = PixelRed,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PixelButton(
                            text = "ROCK",
                            onClick = { onSubmitChoice("Rock") },
                            modifier = Modifier.fillMaxWidth(),
                            bgColor = PixelOrange
                        )
                        PixelButton(
                            text = "PAPER",
                            onClick = { onSubmitChoice("Paper") },
                            modifier = Modifier.fillMaxWidth(),
                            bgColor = PixelCyan
                        )
                        PixelButton(
                            text = "SCISSORS",
                            onClick = { onSubmitChoice("Scissors") },
                            modifier = Modifier.fillMaxWidth(),
                            bgColor = PixelGreen
                        )
                    }
                }
                "WAITING_FOR_OTHER" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "WAITING FOR\nOPPONENT...",
                                style = MaterialTheme.typography.h6,
                                color = PixelYellow,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = ". . .",
                                style = MaterialTheme.typography.h4,
                                color = PixelCyan
                            )
                        }
                    }
                }
                "DRAW_NEXT_ROUND" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DRAW – BE READY FOR NEXT ROUND",
                            style = MaterialTheme.typography.h6,
                            color = PixelYellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                            text = (gameResultMessage ?: "-").uppercase(),
                            style = MaterialTheme.typography.h5,
                            color = PixelYellow,
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
                            text = "LOADING...",
                            style = MaterialTheme.typography.body1,
                            color = PixelLightGray
                        )
                    }
                }
            }
        }
    }
}
