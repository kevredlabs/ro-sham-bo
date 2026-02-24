package com.solanamobile.ktxclientsample.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelLightBlue
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelTeal
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkBlue

@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sectionTitle = MaterialTheme.typography.h5.copy(
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
    val bodyStyle = MaterialTheme.typography.body1.copy(
        fontSize = 9.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )

    PixelScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            PixelOutlinedButton(
                text = "< Back",
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .fillMaxWidth(0.35f),
                buttonHeight = 36.dp,
                borderColor = PixelLightGray,
                textColor = PixelLightGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            PixelShadowText(
                text = "RULES",
                color = PixelCyan,
                shadowColor = PixelTeal,
                style = MaterialTheme.typography.h3.copy(
                    fontSize = 28.sp,
                    letterSpacing = 3.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- HOW TO PLAY ---
            PixelShadowText(
                text = "HOW TO PLAY",
                color = PixelLightBlue,
                shadowColor = PixelDarkBlue,
                style = sectionTitle
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bet SOL against another player in a classic Rock Paper Scissors duel on Solana.",
                style = bodyStyle,
                color = PixelWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. Create a game & set your bet\n" +
                    "2. Share the PIN with your opponent\n" +
                    "3. Both players pick Rock, Paper or Scissors\n" +
                    "4. Winner takes all!",
                style = bodyStyle,
                color = PixelLightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "If it's a draw, a new round starts automatically until someone wins.",
                style = bodyStyle,
                color = PixelLightGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- FEES ---
            PixelShadowText(
                text = "FEES",
                color = PixelYellow,
                shadowColor = PixelTeal,
                style = sectionTitle
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A 3% fee is taken from the total pot when the game is resolved.",
                style = bodyStyle,
                color = PixelWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The remaining 97% goes to the winner. Fees help us keep the game running and improve it.",
                style = bodyStyle,
                color = PixelLightGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- CONTACT ---
            PixelShadowText(
                text = "CONTACT",
                color = PixelLightBlue,
                shadowColor = PixelDarkBlue,
                style = sectionTitle
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Need help or have a question? Reach out to us:",
                style = bodyStyle,
                color = PixelWhite
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelButton(
                text = "Telegram: @kevredlabs",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/kevredlabs"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                bgColor = PixelLightBlue,
                shadowColor = PixelDarkBlue,
                buttonHeight = 48.dp,
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
