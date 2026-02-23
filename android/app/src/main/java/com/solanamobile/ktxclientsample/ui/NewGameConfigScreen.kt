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
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.config.SolanaConfig
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
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
    val amountPerPlayer = (amountSol * SolanaConfig.LAMPORTS_PER_SOL).toLong()
    val balanceLamports = (solBalance * SolanaConfig.LAMPORTS_PER_SOL).toLong()
    val amountValid = amountPerPlayer > 0
    val withinBalance = amountPerPlayer <= balanceLamports
    val canCreate = amountValid && withinBalance && !isLoading

    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            PixelOutlinedButton(
                text = "< Back",
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start).fillMaxWidth(0.35f),
                buttonHeight = 36.dp,
                borderColor = PixelLightGray,
                textColor = PixelLightGray
            )

            Spacer(modifier = Modifier.weight(1f))

            PixelShadowText(
                text = "CONFIGURE",
                color = PixelCyan,
                shadowColor = PixelTeal,
                style = MaterialTheme.typography.h3.copy(
                    fontSize = 24.sp,
                    letterSpacing = 3.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            PixelShadowText(
                text = "GAME",
                color = PixelCyan,
                shadowColor = PixelTeal,
                style = MaterialTheme.typography.h3.copy(
                    fontSize = 24.sp,
                    letterSpacing = 3.sp
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedTextField(
                value = amountSolText,
                onValueChange = { amountSolText = it },
                label = {
                    Text(
                        "Amount (SOL)",
                        color = PixelLightGray,
                        style = MaterialTheme.typography.body1
                    )
                },
                textStyle = MaterialTheme.typography.h5.copy(color = PixelYellow),
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Balance: ${"%.4f".format(solBalance)} SOL",
                style = MaterialTheme.typography.h5,
                color = PixelYellow
            )

            if (!amountValid && amountSolText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Amount must be greater than 0",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error
                )
            } else if (!withinBalance && amountValid) {
                Spacer(modifier = Modifier.height(6.dp))
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
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PixelButton(
                text = if (isLoading) "Creating..." else "Create game",
                onClick = { onCreateGame(amountPerPlayer) },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelCyan,
                buttonHeight = 64.dp,
                textStyle = MaterialTheme.typography.h5,
                borderWidth = 3.dp,
                shadowOffset = 5.dp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
