package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelOrange
import com.solanamobile.ktxclientsample.ui.theme.PixelRed
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun CurrentGameScreen(
    gameId: String,
    gamePhase: String?,
    countdownNumber: Int,
    gameResultMessage: String?,
    isWinner: Boolean?,
    gameAmountPerPlayer: Long,
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
            Spacer(modifier = Modifier.height(20.dp))
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
                "COUNTDOWN" -> {
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
                "RESULT_COUNTDOWN" -> {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val titleStyle = MaterialTheme.typography.h3.copy(
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                            PixelShadowText(
                                text = "CHOOSE YOUR MOVE",
                                color = PixelCyan,
                                shadowColor = PixelTeal,
                                style = titleStyle
                            )

                            if (error.isNotEmpty()) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.body2,
                                    color = PixelRed,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PixelMoveCard(
                                    label = "ROCK",
                                    borderColor = PixelLightBlue,
                                    shadowColor = PixelDarkBlue,
                                    onClick = { onSubmitChoice("Rock") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                ) {
                                    PixelRockIcon(modifier = Modifier.size(64.dp))
                                }
                                PixelMoveCard(
                                    label = "PAPER",
                                    borderColor = PixelCyan,
                                    shadowColor = PixelTeal,
                                    onClick = { onSubmitChoice("Paper") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                ) {
                                    PixelPaperIcon(modifier = Modifier.size(64.dp))
                                }
                                PixelMoveCard(
                                    label = "SCISSORS",
                                    borderColor = PixelOrange,
                                    shadowColor = PixelRed,
                                    onClick = { onSubmitChoice("Scissors") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                ) {
                                    PixelScissorsIcon(modifier = Modifier.size(64.dp))
                                }
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
                    val betSol = gameAmountPerPlayer / 1_000_000_000.0
                    val potSol = betSol * 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val resultTitle = when (isWinner) {
                                true -> "YOU WON"
                                false -> "YOU LOST"
                                null -> "DRAW"
                            }
                            val resultColor = when (isWinner) {
                                true -> PixelLightBlue
                                false -> PixelOrange
                                null -> PixelYellow
                            }
                            val resultShadow = when (isWinner) {
                                true -> PixelDarkBlue
                                false -> PixelRed
                                null -> PixelTeal
                            }
                            val titleStyle = MaterialTheme.typography.h3.copy(
                                fontSize = 40.sp,
                                letterSpacing = 4.sp
                            )
                            PixelShadowText(
                                text = resultTitle,
                                color = resultColor,
                                shadowColor = resultShadow,
                                style = titleStyle
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isWinner == null) {
                                Text(
                                    text = "BE READY FOR NEXT ROUND",
                                    style = MaterialTheme.typography.h6.copy(
                                        letterSpacing = 2.sp
                                    ),
                                    color = PixelCyan,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                val resultStyle = MaterialTheme.typography.body1.copy(
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                                val resultMsg = (gameResultMessage ?: "").uppercase()
                                if (resultMsg.isNotEmpty()) {
                                    Text(
                                        text = resultMsg,
                                        style = resultStyle,
                                        color = PixelLightGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                if (betSol > 0) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "BET: ${"%.3f".format(betSol)} SOL",
                                            style = resultStyle,
                                            color = PixelLightGray,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        val label = if (isWinner) "PROFIT" else "LOSS"
                                        val sign = if (isWinner) "+" else "-"
                                        val amountDisplay = if (isWinner) potSol else betSol
                                        val amountFormatted = "%.3f".format(amountDisplay)
                                        Text(
                                            text = "$label: $sign $amountFormatted SOL",
                                            style = resultStyle,
                                            color = if (isWinner) PixelCyan else PixelOrange,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
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

