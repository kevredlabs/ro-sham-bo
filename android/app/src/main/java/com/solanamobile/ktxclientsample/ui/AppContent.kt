package com.solanamobile.ktxclientsample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solanamobile.ktxclientsample.viewmodel.SampleViewModel

/**
 * Root content: shows wallet connect screen when not connected,
 * Main menu when connected (via Mobile Wallet Adapter).
 */
@Composable
fun AppContent(
    intentSender: ActivityResultSender,
    viewModel: SampleViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewState.collectAsState().value
    val isConnected = viewState.userAddress.isNotEmpty()

    if (isConnected) {
        MainMenuScreen(
            userAddress = viewState.userAddress,
            solBalance = viewState.solBalance,
            network = "Devnet",
            onNewGame = { /* TODO */ },
            onJoinGame = { /* TODO */ },
            onDisconnect = { viewModel.disconnect(intentSender) }
        )
    } else {
        WalletConnectScreen(intentSender = intentSender, viewModel = viewModel)
    }
}
