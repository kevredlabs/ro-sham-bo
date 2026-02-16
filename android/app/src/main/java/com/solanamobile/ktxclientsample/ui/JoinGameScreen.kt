package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PIN_LENGTH = 4

/**
 * Screen to join an existing game by entering a 4-digit PIN.
 * Uses a numeric-only keypad (digits 0-9) and an Enter button to validate.
 */
@Composable
fun JoinGameScreen(
    isLoading: Boolean,
    error: String,
    onEnter: (String) -> Unit,
    onBack: () -> Unit
) {
    var pinDigits by remember { mutableStateOf("") }
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
            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(0.dp),
                onClick = onBack
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter game PIN",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PIN display: show entered digits, then dots for remaining
            val display = (pinDigits + "____").take(PIN_LENGTH).map { c ->
                if (c in '0'..'9') c else '•'
            }.joinToString(" ")
            Text(
                text = display,
                style = MaterialTheme.typography.h4.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center
            )

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric keypad: 1-9, 0, backspace, then Enter
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf('1', '2', '3').forEach { digit ->
                        DigitKey(
                            digit = digit,
                            onClick = { pinDigits = (pinDigits + digit).take(PIN_LENGTH) },
                            enabled = !isLoading && pinDigits.length < PIN_LENGTH
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf('4', '5', '6').forEach { digit ->
                        DigitKey(
                            digit = digit,
                            onClick = { pinDigits = (pinDigits + digit).take(PIN_LENGTH) },
                            enabled = !isLoading && pinDigits.length < PIN_LENGTH
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf('7', '8', '9').forEach { digit ->
                        DigitKey(
                            digit = digit,
                            onClick = { pinDigits = (pinDigits + digit).take(PIN_LENGTH) },
                            enabled = !isLoading && pinDigits.length < PIN_LENGTH
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { pinDigits = pinDigits.dropLast(1) },
                        enabled = !isLoading && pinDigits.isNotEmpty(),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace"
                        )
                    }
                    DigitKey(
                        digit = '0',
                        onClick = { pinDigits = (pinDigits + '0').take(PIN_LENGTH) },
                        enabled = !isLoading && pinDigits.length < PIN_LENGTH
                    )
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = { onEnter(pinDigits) },
                enabled = !isLoading && pinDigits.length == PIN_LENGTH
            ) {
                Text(if (isLoading) "Joining…" else "Enter")
            }
        }
    }
}

@Composable
private fun DigitKey(
    digit: Char,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(64.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.h5
        )
    }
}
