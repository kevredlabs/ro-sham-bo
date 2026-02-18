package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solanamobile.ktxclientsample.config.SolanaConfig
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelGreen
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow

@Composable
fun NewGameConfigScreen(
    solBalance: Double,
    isLoading: Boolean,
    error: String,
    onCreateGame: (Long) -> Unit,
    onBack: () -> Unit
) {
    var amountSolText by remember { mutableStateOf("0.1") }
    val amountSol = amountSolText.toDoubleOrNull() ?: 0.0
    val amountLamports = (amountSol * SolanaConfig.LAMPORTS_PER_SOL).toLong()
    val balanceLamports = (solBalance * SolanaConfig.LAMPORTS_PER_SOL).toLong()
    val amountValid = amountLamports > 0
    val withinBalance = amountLamports <= balanceLamports
    val canCreate = amountValid && withinBalance && !isLoading

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
                text = "CONFIGURE GAME",
                style = MaterialTheme.typography.h6,
                color = PixelCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Balance: $solBalance SOL",
                style = MaterialTheme.typography.body2,
                color = PixelYellow
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = amountSolText,
                onValueChange = { amountSolText = it },
                label = { Text("Amount (SOL)", color = PixelLightGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                    textColor = PixelYellow,
                    cursorColor = PixelCyan,
                    focusedBorderColor = PixelCyan,
                    unfocusedBorderColor = PixelLightGray,
                    focusedLabelColor = PixelCyan,
                    unfocusedLabelColor = PixelLightGray
                )
            )

            if (!amountValid && amountSolText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Amount must be greater than 0",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error
                )
            } else if (!withinBalance && amountValid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Amount exceeds balance",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error
                )
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

            PixelButton(
                text = if (isLoading) "Creating..." else "Create game",
                onClick = { onCreateGame(amountLamports) },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelGreen
            )
        }
    }
}
