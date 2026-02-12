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
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solanamobile.ktxclientsample.viewmodel.SampleViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Wallet connection screen via Mobile Wallet Adapter (Seeker / MWA-compatible wallet).
 * Shown only when the user is not connected; after successful sign-in, navigation goes to Hello World.
 */
@Composable
fun WalletConnectScreen(
    intentSender: ActivityResultSender,
    viewModel: SampleViewModel = hiltViewModel()
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colors.background
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
                        text = "Seeker RPS",
                        style = MaterialTheme.typography.h4,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your Seeker wallet to continue",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (!viewState.walletFound) {
                        Text(
                            text = "No MWA-compatible wallet found on this device.",
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = { viewModel.signIn(intentSender) },
                            enabled = !viewState.isLoading
                        ) {
                            Text("Connect wallet")
                        }
                    }
                }

                if (viewState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}
