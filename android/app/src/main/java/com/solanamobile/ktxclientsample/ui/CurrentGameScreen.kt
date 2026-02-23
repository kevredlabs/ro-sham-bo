package com.solanamobile.ktxclientsample.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow
import kotlinx.coroutines.delay

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
                    RevealCollisionScreen(
                        countdownNumber = countdownNumber,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
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
                            horizontalAlignment = Alignment.CenterHorizontally
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
                                if (betSol > 0) {
                                    Text(
                                        text = "BET: ${"%.3f".format(betSol)} SOL",
                                        style = MaterialTheme.typography.body1.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = PixelLightGray
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val label = if (isWinner) "PROFIT" else "LOSS"
                                    val sign = if (isWinner) "+" else "-"
                                    val potFormatted = "%.3f".format(potSol)
                                    Text(
                                        text = "$label: $sign $potFormatted SOL",
                                        style = MaterialTheme.typography.h5.copy(
                                            fontSize = 22.sp,
                                            letterSpacing = 2.sp
                                        ),
                                        color = if (isWinner) PixelCyan else PixelOrange
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = (gameResultMessage ?: "").uppercase(),
                                    style = MaterialTheme.typography.body1,
                                    color = PixelLightGray,
                                    textAlign = TextAlign.Center
                                )
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

@Composable
private fun RevealCollisionScreen(
    countdownNumber: Int,
    modifier: Modifier = Modifier
) {
    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "collision"
    )

    LaunchedEffect(countdownNumber) {
        animProgress = 0f
        delay(100)
        animProgress = 1f
    }

    val showImpact = countdownNumber <= 1
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shake by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(280.dp, 200.dp)
                .then(
                    if (showImpact) Modifier.offset(x = shake.dp) else Modifier
                )
        ) {
            val w = size.width
            val h = size.height
            val px = w / 28f
            val centerX = w / 2f
            val centerY = h / 2f

            val approach = if (showImpact) 1f else animatedProgress
            val leftFistX = centerX - 60.dp.toPx() + (40.dp.toPx() * approach)
            val rightFistX = centerX + 60.dp.toPx() - (40.dp.toPx() * approach)
            val fistY = centerY - 3 * px

            drawFist(leftFistX - 3 * px, fistY, px, PixelCyan, PixelTeal, false)
            drawFist(rightFistX - 3 * px, fistY, px, PixelOrange, PixelRed, true)

            if (showImpact) {
                val impactAlpha = if (countdownNumber == 0) 0.6f else 1f
                drawImpactCloud(centerX, centerY, px, impactAlpha)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (countdownNumber > 0) {
                Text(
                    text = countdownNumber.toString(),
                    style = MaterialTheme.typography.h3.copy(
                        fontSize = 48.sp,
                        letterSpacing = 4.sp
                    ),
                    color = PixelYellow
                )
            }
        }
    }
}

private fun DrawScope.drawFist(
    x: Float, y: Float, px: Float,
    mainColor: Color, shadowColor: Color,
    mirrored: Boolean
) {
    fun pixel(col: Int, row: Int, color: Color) {
        val actualCol = if (mirrored) 5 - col else col
        drawRect(
            color = color,
            topLeft = Offset(x + actualCol * px, y + row * px),
            size = Size(px, px)
        )
    }
    // Row 0: top of fist
    pixel(1, 0, mainColor); pixel(2, 0, mainColor); pixel(3, 0, mainColor); pixel(4, 0, mainColor)
    // Row 1
    pixel(0, 1, mainColor); pixel(1, 1, mainColor); pixel(2, 1, mainColor)
    pixel(3, 1, mainColor); pixel(4, 1, mainColor); pixel(5, 1, mainColor)
    // Row 2
    pixel(0, 2, mainColor); pixel(1, 2, mainColor); pixel(2, 2, mainColor)
    pixel(3, 2, mainColor); pixel(4, 2, mainColor); pixel(5, 2, mainColor)
    // Row 3: wider
    pixel(0, 3, mainColor); pixel(1, 3, mainColor); pixel(2, 3, mainColor)
    pixel(3, 3, mainColor); pixel(4, 3, mainColor); pixel(5, 3, mainColor)
    // Row 4
    pixel(0, 4, shadowColor); pixel(1, 4, mainColor); pixel(2, 4, mainColor)
    pixel(3, 4, mainColor); pixel(4, 4, shadowColor); pixel(5, 4, shadowColor)
    // Row 5: bottom
    pixel(1, 5, shadowColor); pixel(2, 5, shadowColor); pixel(3, 5, shadowColor); pixel(4, 5, shadowColor)
}

private fun DrawScope.drawImpactCloud(
    cx: Float, cy: Float, px: Float,
    alpha: Float
) {
    val sparkColor = PixelYellow.copy(alpha = alpha)
    val whiteColor = PixelWhite.copy(alpha = alpha * 0.8f)

    // Star/burst pattern around collision point
    val offsets = listOf(
        -2f to -3f, 2f to -3f,
        -3f to -1f, 3f to -1f,
        -4f to 0f, 4f to 0f,
        -3f to 1f, 3f to 1f,
        -2f to 3f, 2f to 3f,
        0f to -4f, 0f to 4f,
        -1f to -2f, 1f to -2f,
        -1f to 2f, 1f to 2f,
    )
    for ((dx, dy) in offsets) {
        drawRect(
            color = sparkColor,
            topLeft = Offset(cx + dx * px - px / 2, cy + dy * px - px / 2),
            size = Size(px, px)
        )
    }
    // Center white flash
    val centerOffsets = listOf(
        -1f to -1f, 0f to -1f, 1f to -1f,
        -1f to 0f, 0f to 0f, 1f to 0f,
        -1f to 1f, 0f to 1f, 1f to 1f,
    )
    for ((dx, dy) in centerOffsets) {
        drawRect(
            color = whiteColor,
            topLeft = Offset(cx + dx * px - px / 2, cy + dy * px - px / 2),
            size = Size(px, px)
        )
    }
}
