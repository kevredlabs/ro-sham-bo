package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelBlack
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkGray
import com.solanamobile.ktxclientsample.ui.theme.PixelGray
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

private const val PIN_LENGTH = 4

@Composable
fun JoinGameScreen(
    isLoading: Boolean,
    error: String,
    onEnter: (String) -> Unit,
    onBack: () -> Unit
) {
    var pinDigits by remember { mutableStateOf("") }

    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelOutlinedButton(
                text = "< Back",
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start).fillMaxWidth(0.35f),
                buttonHeight = 36.dp,
                borderColor = PixelLightGray,
                textColor = PixelLightGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ENTER GAME PIN",
                style = MaterialTheme.typography.h6,
                color = PixelCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PIN display
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                for (i in 0 until PIN_LENGTH) {
                    val char = pinDigits.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, if (char != null) PixelYellow else PixelGray, RectangleShape)
                            .background(PixelDarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char?.toString() ?: "_",
                            style = MaterialTheme.typography.h4.copy(fontSize = 20.sp),
                            color = if (char != null) PixelYellow else PixelGray
                        )
                    }
                }
            }

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Numeric keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9')
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit ->
                            PixelDigitKey(
                                digit = digit,
                                onClick = { pinDigits = (pinDigits + digit).take(PIN_LENGTH) },
                                enabled = !isLoading && pinDigits.length < PIN_LENGTH
                            )
                        }
                    }
                }
                // Last row: backspace, 0, empty
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, if (pinDigits.isNotEmpty()) PixelLightGray else PixelGray, RectangleShape)
                            .then(
                                if (!isLoading && pinDigits.isNotEmpty())
                                    Modifier.clickable { pinDigits = pinDigits.dropLast(1) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = if (pinDigits.isNotEmpty()) PixelLightGray else PixelGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    PixelDigitKey(
                        digit = '0',
                        onClick = { pinDigits = (pinDigits + '0').take(PIN_LENGTH) },
                        enabled = !isLoading && pinDigits.length < PIN_LENGTH
                    )
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PixelButton(
                text = if (isLoading) "Joining..." else "Enter",
                onClick = { onEnter(pinDigits) },
                enabled = !isLoading && pinDigits.length == PIN_LENGTH,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PixelDigitKey(
    digit: Char,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val borderColor = if (enabled) PixelCyan else PixelGray
    val textColor = if (enabled) PixelWhite else PixelGray

    Box(
        modifier = Modifier
            .size(56.dp)
            .border(2.dp, borderColor, RectangleShape)
            .background(if (enabled) Color.Transparent else Color.Transparent)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.h5,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
