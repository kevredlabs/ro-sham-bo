package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow
import com.solanamobile.ktxclientsample.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WalletConnectScreen(
    intentSender: ActivityResultSender,
    viewModel: GameViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadConnection()
    }

    LaunchedEffect(Unit) {
        viewModel.viewState.collectLatest { state ->
            if (state.error.isNotEmpty()) {
                snackbarHostState.showSnackbar(state.error, "DISMISS").let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        PixelScreen(
            modifier = Modifier.padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SEEKER RPS",
                        style = MaterialTheme.typography.h3,
                        color = PixelCyan,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connect your wallet\nto start playing",
                        style = MaterialTheme.typography.body1,
                        color = PixelYellow,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    if (!viewState.walletFound) {
                        Text(
                            text = "No MWA wallet found\non this device.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        PixelButton(
                            text = "Connect Wallet",
                            onClick = { viewModel.signIn(intentSender) },
                            enabled = !viewState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (viewState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        color = PixelCyan
                    )
                }
            }
        }
    }
}
