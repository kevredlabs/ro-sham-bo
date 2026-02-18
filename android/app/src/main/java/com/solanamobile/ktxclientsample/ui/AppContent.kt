package com.solanamobile.ktxclientsample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solanamobile.ktxclientsample.viewmodel.GameViewModel

/**
 * Root content: shows wallet connect screen when not connected,
 * Main menu when connected (via Mobile Wallet Adapter).
 */
@Composable
fun AppContent(
    intentSender: ActivityResultSender,
    viewModel: GameViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewState.collectAsState().value
    val isConnected = viewState.userAddress.isNotEmpty()

    if (isConnected) {
        val joinedGameId = viewState.joinedGameId
        val gamePin = viewState.gamePin
        val showJoinGame = viewState.showJoinGameScreen
        when {
            joinedGameId != null -> CurrentGameScreen(
                gameId = joinedGameId,
                gamePhase = viewState.gamePhase,
                countdownNumber = viewState.countdownNumber,
                gameResultMessage = viewState.gameResultMessage,
                error = viewState.error,
                onScreenVisible = { viewModel.onCurrentGameScreenVisible(it) },
                onSubmitChoice = { viewModel.submitChoice(it) },
                onBack = { viewModel.backFromCurrentGame() }
            )
            gamePin != null -> NewGameScreen(
                pin = gamePin,
                onBack = { viewModel.backFromNewGame() }
            )
            viewState.showNewGameConfigScreen -> NewGameConfigScreen(
                solBalance = viewState.solBalance,
                isLoading = viewState.isLoading,
                error = viewState.error,
                onCreateGame = { amountLamports -> viewModel.startNewGame(intentSender, amountLamports) },
                onBack = { viewModel.backFromNewGameConfig() }
            )
            showJoinGame -> JoinGameScreen(
                isLoading = viewState.isLoading,
                error = viewState.error,
                onEnter = { viewModel.joinGame(it) },
                onBack = { viewModel.backFromJoinGame() }
            )
            else -> MainMenuScreen(
                userAddress = viewState.userAddress,
                solBalance = viewState.solBalance,
                network = "Devnet",
                onNewGame = { viewModel.enterNewGameConfig() },
                onJoinGame = { viewModel.enterJoinGame() },
                onDisconnect = { viewModel.disconnect(intentSender) }
            )
        }
    } else {
        WalletConnectScreen(intentSender = intentSender, viewModel = viewModel)
    }
}
