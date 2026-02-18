package com.solanamobile.ktxclientsample.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.AdapterOperations
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient.AuthorizationResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.mobilewalletadapter.common.signin.SignInWithSolana
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solanamobile.ktxclientsample.usecase.Connected
import com.solanamobile.ktxclientsample.usecase.GameApiUseCase
import com.solanamobile.ktxclientsample.usecase.NotConnected
import com.solanamobile.ktxclientsample.usecase.PersistanceUseCase
import com.solanamobile.ktxclientsample.usecase.EscrowUseCase
import com.solanamobile.ktxclientsample.usecase.SolanaRpcUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.base.Base58
import javax.inject.Inject

data class GameViewState(
    val isLoading: Boolean = false,
    val solBalance: Double = 0.0,
    val userAddress: String = "",
    val userLabel: String = "",
    val error: String = "",
    val walletFound: Boolean = true,
    /** When non-null, show NewGameScreen with this PIN (game was just created). */
    val gamePin: String? = null,
    val gameId: String? = null,
    /** When true, show NewGameConfigScreen (set amount then create game). */
    val showNewGameConfigScreen: Boolean = false,
    /** When true, show JoinGameScreen (enter PIN to join). */
    val showJoinGameScreen: Boolean = false,
    /** When non-null, show CurrentGameScreen (user joined this game). */
    val joinedGameId: String? = null,
    /** Current game screen phase: COUNTDOWN, SELECTION, WAITING_FOR_OTHER, DRAW_NEXT_ROUND, RESULT_COUNTDOWN, RESULT. */
    val gamePhase: String? = null,
    /** Countdown value to display (3, 2, 1). */
    val countdownNumber: Int = 3,
    /** Result message after both played: "rock beats scissors, winner is: pubkey". */
    val gameResultMessage: String? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val solanaRpcUseCase: SolanaRpcUseCase,
    private val persistanceUseCase: PersistanceUseCase,
    private val gameApiUseCase: GameApiUseCase,
    private val escrowUseCase: EscrowUseCase
): ViewModel() {

    private fun GameViewState.updateViewState() {
        _state.update { this }
    }

    private val _state = MutableStateFlow(GameViewState())

    val viewState: StateFlow<GameViewState>
        get() = _state

    fun loadConnection() {
        val persistedConn = persistanceUseCase.getWalletConnection()

        if (persistedConn is Connected) {
            viewModelScope.launch {
                _state.value.copy(
                    userAddress = persistedConn.publicKey.base58(),
                    userLabel = persistedConn.accountLabel,
                    solBalance = solanaRpcUseCase.getBalance(persistedConn.publicKey)
                ).updateViewState()
            }

            walletAdapter.authToken = persistedConn.authToken
        }
    }

    fun signIn(sender: ActivityResultSender) {
        viewModelScope.launch {
            connect(sender) {}
            // Note: should check the signature here of the signInResult to verify it matches the
            // account and expected signed message.
        }
    }

    /** Opens the New Game config screen (set amount). */
    fun enterNewGameConfig() {
        _state.update { it.copy(showNewGameConfigScreen = true, error = "") }
    }

    /** Returns from New Game config to main menu. */
    fun backFromNewGameConfig() {
        _state.update { it.copy(showNewGameConfigScreen = false, error = "") }
    }

    /**
     * Creates a new game: 1) API create (game_id, pin), 2) build create_game instruction with [amountLamports],
     * 3) wallet signs and sends tx via Mobile Wallet Adapter, 4) on success show PIN screen.
     */
    fun startNewGame(sender: ActivityResultSender, amountLamports: Long) {
        val address = _state.value.userAddress
        if (address.isEmpty()) {
            Log.w(TAG, "startNewGame: no wallet connected")
            _state.update { it.copy(error = "Connect wallet first") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "startNewGame: creating game via API for $address")
            _state.update { it.copy(isLoading = true, error = "") }
            val apiResult = gameApiUseCase.createGame(address)
            apiResult
                .onFailure { e ->
                    Log.e(TAG, "startNewGame: API create failed", e)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to create game"
                        )
                    }
                    return@launch
                }
            val result = apiResult.getOrNull() ?: return@launch
            val gameId = result.gameId
            val pin = result.pin
            Log.d(TAG, "startNewGame: API ok gameId=$gameId pin=$pin")
            val gameIdBytes = escrowUseCase.gameIdToBytes(gameId)
            if (gameIdBytes == null) {
                Log.e(TAG, "startNewGame: invalid game_id from API")
                _state.update {
                    it.copy(isLoading = false, error = "Invalid game id from API")
                }
                return@launch
            }
            val creator = SolanaPublicKey.from(address)
            val createGameIx = escrowUseCase.buildCreateGameInstruction(creator, gameIdBytes, amountLamports)
            Log.d(TAG, "startNewGame: createGameIx=$createGameIx")
            if (createGameIx == null) {
                Log.e(TAG, "startNewGame: buildCreateGameInstruction returned null")
                _state.update {
                    it.copy(isLoading = false, error = "Failed to build create_game instruction")
                }
                return@launch
            }
            Log.d(TAG, "startNewGame: instruction built, opening wallet session")
            try {
                val txResult = connect(sender) { authResult ->
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "startNewGame: fetching latest blockhash before building tx")
                        val blockHash = solanaRpcUseCase.getLatestBlockHash()
                        Log.d(TAG, "startNewGame: blockhash=$blockHash, building transaction")
                        val createGameMessage = Message.Builder()
                            .addInstruction(createGameIx)
                            .setRecentBlockhash(blockHash)
                            .build()
                        
                        val unsignedCreateGameTx = Transaction(createGameMessage)
                        Log.d(TAG, "startNewGame: unsignedCreateGameTx=${unsignedCreateGameTx.serialize()}")
                        Log.d(TAG, "startNewGame: sending transaction to wallet (signAndSendTransactions)")
                        signAndSendTransactions(arrayOf(unsignedCreateGameTx.serialize()))
                    }
                }
                Log.d(TAG, "startNewGame: connect returned result=${txResult::class.simpleName}" + (if (txResult is TransactionResult.Failure) " message=${txResult.message}" else ""))
                when (txResult) {
                    is TransactionResult.Success -> {
                        val txSignatureBytes = txResult.successPayload?.signatures?.first()
                        txSignatureBytes?.let {
                            Log.i(TAG, "startNewGame: tx success signature=${Base58.encode(it)}")
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showNewGameConfigScreen = false,
                                    gamePin = pin,
                                    gameId = gameId,
                                    error = ""
                                )
                            }
                            startPollingGameState(gameId)
                        } ?: run {
                            Log.w(TAG, "startNewGame: Success but no signature in payload")
                            _state.update {
                                it.copy(isLoading = false, error = "Transaction sent but no signature")
                            }
                        }
                    }
                    is TransactionResult.NoWalletFound -> {
                        Log.w(TAG, "startNewGame: NoWalletFound ${txResult.message}")
                        _state.update {
                            it.copy(isLoading = false, walletFound = false, error = txResult.message)
                        }
                    }
                    is TransactionResult.Failure -> {
                        Log.e(TAG, "startNewGame: Failure message=${txResult.message}")
                        _state.update {
                            it.copy(isLoading = false, error = txResult.message)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "startNewGame: exception during connect/signAndSend", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Transaction failed: ${e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "SeekerRPS"
    }

    /** Returns from NewGameScreen (waiting) to main menu. Stops polling. */
    fun backFromNewGame() {
        _state.update { it.copy(gamePin = null, gameId = null) }
    }

    /** Polls game state every second; when a second player joins, navigates to CurrentGameScreen. */
    private fun startPollingGameState(gameId: String) {
        viewModelScope.launch {
            while (_state.value.gameId == gameId) {
                delay(1000L)
                if (_state.value.gameId != gameId) break
                gameApiUseCase.getGame(gameId)
                    .onSuccess { gameState ->
                        if (gameState.status == "active" || gameState.joinerPubkey != null) {
                            _state.update {
                                it.copy(
                                    joinedGameId = gameId,
                                    gamePin = null,
                                    gameId = null
                                )
                            }
                            return@launch
                        }
                    }
            }
        }
    }

    /** Navigate to JoinGameScreen (enter PIN). */
    fun enterJoinGame() {
        _state.update { it.copy(showJoinGameScreen = true, error = "") }
    }

    /** Returns from JoinGameScreen to main menu. */
    fun backFromJoinGame() {
        _state.update { it.copy(showJoinGameScreen = false, error = "") }
    }

    /**
     * Joins a game with the given 4-digit PIN: API join, then on-chain join_game tx.
     * Only navigates to CurrentGameScreen when the transaction succeeds.
     */
    fun joinGame(sender: ActivityResultSender, pin: String) {
        val address = _state.value.userAddress
        if (address.isEmpty()) {
            Log.w(TAG, "joinGame: no wallet connected")
            _state.update { it.copy(error = "Connect wallet first") }
            return
        }
        val trimmedPin = pin.take(4).filter { it.isDigit() }
        if (trimmedPin.length != 4) {
            _state.update { it.copy(error = "PIN must be 4 digits") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "joinGame: joining via API pin=**** joiner=$address")
            _state.update { it.copy(isLoading = true, error = "") }
            val apiResult = gameApiUseCase.joinGame(trimmedPin, address)
            apiResult.onFailure { e ->
                Log.e(TAG, "joinGame: API join failed", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to join game"
                    )
                }
                return@launch
            }
            val result = apiResult.getOrNull() ?: return@launch
            val gameId = result.gameId
            Log.d(TAG, "joinGame: API ok gameId=$gameId")
            Log.d(TAG, "joinGame: fetching game for creator_pubkey")
            val getGameResult = gameApiUseCase.getGame(gameId)
            getGameResult.onFailure { e ->
                Log.e(TAG, "joinGame: getGame failed", e)
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load game")
                }
                return@launch
            }
            val gameState = getGameResult.getOrNull() ?: return@launch
            val creatorPubkey = gameState.creatorPubkey
            if (creatorPubkey.isNullOrEmpty()) {
                Log.e(TAG, "joinGame: missing creator_pubkey in game")
                _state.update {
                    it.copy(isLoading = false, error = "Invalid game data (missing creator)")
                }
                return@launch
            }
            Log.d(TAG, "joinGame: creator=$creatorPubkey")
            val gameIdBytes = escrowUseCase.gameIdToBytes(gameId)
            if (gameIdBytes == null) {
                Log.e(TAG, "joinGame: invalid game_id from API")
                _state.update {
                    it.copy(isLoading = false, error = "Invalid game id from API")
                }
                return@launch
            }
            val joiner = SolanaPublicKey.from(address)
            val creator = SolanaPublicKey.from(creatorPubkey)
            val joinGameIx = escrowUseCase.buildJoinGameInstruction(joiner, creator, gameIdBytes)
            Log.d(TAG, "joinGame: joinGameIx=$joinGameIx")
            if (joinGameIx == null) {
                Log.e(TAG, "joinGame: buildJoinGameInstruction returned null")
                _state.update {
                    it.copy(isLoading = false, error = "Failed to build join_game instruction")
                }
                return@launch
            }
            Log.d(TAG, "joinGame: instruction built, opening wallet session")
            try {
                val txResult = connect(sender) { authResult ->
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "joinGame: fetching latest blockhash before building tx")
                        val blockHash = solanaRpcUseCase.getLatestBlockHash()
                        Log.d(TAG, "joinGame: blockhash=$blockHash, building transaction")
                        val message = Message.Builder()
                            .addInstruction(joinGameIx)
                            .setRecentBlockhash(blockHash)
                            .build()
                        val unsignedTx = Transaction(message)
                        Log.d(TAG, "joinGame: sending transaction to wallet (signAndSendTransactions)")
                        signAndSendTransactions(arrayOf(unsignedTx.serialize()))
                    }
                }
                Log.d(TAG, "joinGame: connect returned result=${txResult::class.simpleName}" + (if (txResult is TransactionResult.Failure) " message=${txResult.message}" else ""))
                when (txResult) {
                    is TransactionResult.Success -> {
                        val txSignatureBytes = txResult.successPayload?.signatures?.first()
                        txSignatureBytes?.let {
                            Log.i(TAG, "joinGame: tx success signature=${Base58.encode(it)}")
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showJoinGameScreen = false,
                                    joinedGameId = gameId,
                                    error = ""
                                )
                            }
                        } ?: run {
                            Log.w(TAG, "joinGame: Success but no signature in payload")
                            _state.update {
                                it.copy(isLoading = false, error = "Transaction sent but no signature")
                            }
                        }
                    }
                    is TransactionResult.NoWalletFound -> {
                        Log.w(TAG, "joinGame: NoWalletFound ${txResult.message}")
                        _state.update {
                            it.copy(isLoading = false, walletFound = false, error = txResult.message)
                        }
                    }
                    is TransactionResult.Failure -> {
                        Log.e(TAG, "joinGame: Failure message=${txResult.message}")
                        _state.update {
                            it.copy(isLoading = false, error = txResult.message)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "joinGame: exception during connect/signAndSend", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Transaction failed: ${e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    /** Returns from CurrentGameScreen to main menu. */
    fun backFromCurrentGame() {
        _state.update {
            it.copy(
                joinedGameId = null,
                gamePhase = null,
                countdownNumber = 3,
                gameResultMessage = null
            )
        }
    }

    /** Called when CurrentGameScreen is shown. Starts initial 3-2-1 countdown then SELECTION. */
    fun onCurrentGameScreenVisible(gameId: String) {
        val current = _state.value
        if (current.gamePhase != null) return
        _state.update {
            it.copy(gamePhase = "COUNTDOWN", countdownNumber = 3)
        }
        viewModelScope.launch {
            for (n in listOf(3, 2, 1)) {
                if (_state.value.joinedGameId != gameId) return@launch
                _state.update { it.copy(countdownNumber = n) }
                delay(1000L)
            }
            if (_state.value.joinedGameId != gameId) return@launch
            _state.update {
                it.copy(gamePhase = "SELECTION", countdownNumber = 0)
            }
        }
    }

    /** Submits RPS choice and starts polling until both players have chosen (or draw → next round). */
    fun submitChoice(choice: String) {
        val gameId = _state.value.joinedGameId ?: return
        val pubkey = _state.value.userAddress
        if (pubkey.isEmpty()) return
        val normalized = choice.lowercase()
        if (normalized !in listOf("rock", "paper", "scissors")) return
        viewModelScope.launch {
            _state.update { it.copy(gamePhase = "WAITING_FOR_OTHER", error = "") }
            gameApiUseCase.submitChoice(gameId, pubkey, normalized)
                .onSuccess { gameState ->
                    if (gameState.roundClearedForDraw) {
                        startDrawThenSelection(gameId)
                    } else {
                        startPollingUntilBothChosen(gameId)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            gamePhase = "SELECTION",
                            error = e.message ?: "Failed to submit choice"
                        )
                    }
                }
        }
    }

    private fun startDrawThenSelection(gameId: String) {
        viewModelScope.launch {
            _state.update { it.copy(gamePhase = "DRAW_NEXT_ROUND") }
            delay(2000L)
            if (_state.value.joinedGameId != gameId) return@launch
            _state.update { it.copy(gamePhase = "SELECTION") }
        }
    }

    private fun startPollingUntilBothChosen(gameId: String) {
        viewModelScope.launch {
            while (_state.value.joinedGameId == gameId && _state.value.gamePhase == "WAITING_FOR_OTHER") {
                delay(1000L)
                if (_state.value.joinedGameId != gameId || _state.value.gamePhase != "WAITING_FOR_OTHER") break
                gameApiUseCase.getGame(gameId)
                    .onSuccess { gameState ->
                        if (gameState.roundClearedForDraw) {
                            startDrawThenSelection(gameId)
                            return@launch
                        }
                        val bothChosen = gameState.creatorChoice != null && gameState.joinerChoice != null
                        if (bothChosen) {
                            if (gameState.winnerPubkey != null && gameState.status == "finished") {
                                val message = buildResultMessage(gameState)
                                _state.update {
                                    it.copy(
                                        gamePhase = "RESULT_COUNTDOWN",
                                        countdownNumber = 3,
                                        gameResultMessage = message
                                    )
                                }
                                startResultCountdown(gameId)
                                return@launch
                            }
                            // Draw: both chose but no winner (round will be cleared by API)
                            startDrawThenSelection(gameId)
                            return@launch
                        }
                    }
            }
        }
    }

    private fun buildResultMessage(gameState: com.solanamobile.ktxclientsample.usecase.GameState): String {
        val winner = gameState.winnerPubkey
        val c = gameState.creatorChoice ?: ""
        val j = gameState.joinerChoice ?: ""
        return when {
            winner == null -> "Draw: $c vs $j"
            winner == gameState.creatorPubkey -> "$c beats $j, winner is: $winner"
            else -> "$j beats $c, winner is: $winner"
        }
    }

    private fun startResultCountdown(gameId: String) {
        viewModelScope.launch {
            for (n in listOf(3, 2, 1)) {
                if (_state.value.joinedGameId != gameId) return@launch
                _state.update { it.copy(countdownNumber = n) }
                delay(1000L)
            }
            if (_state.value.joinedGameId != gameId) return@launch
            _state.update {
                it.copy(gamePhase = "RESULT", countdownNumber = 0)
            }
        }
    }

    fun disconnect(sender: ActivityResultSender) {
        val conn = persistanceUseCase.getWalletConnection()
        if (conn is Connected) {
            viewModelScope.launch {
                persistanceUseCase.clearConnection()
                val disconnectError = when (val result = walletAdapter.disconnect(sender)) {
                    is TransactionResult.Success -> null
                    is TransactionResult.NoWalletFound -> null
                    is TransactionResult.Failure -> result.message
                }
                _state.value.copy(
                    isLoading = false,
                    solBalance = 0.0,
                    userAddress = "",
                    userLabel = "",
                    error = disconnectError ?: "",
                    gamePin = null,
                    gameId = null,
                    showNewGameConfigScreen = false,
                    showJoinGameScreen = false,
                    joinedGameId = null,
                    gamePhase = null,
                    countdownNumber = 3,
                    gameResultMessage = null
                ).updateViewState()
            }
        }
    }

    private suspend fun <T> connect(sender: ActivityResultSender,
                                    block: suspend AdapterOperations.(authResult: AuthorizationResult) -> T): TransactionResult<T> =
        withContext(viewModelScope.coroutineContext) {
            _state.value.copy(
                isLoading = true,
                error = ""
            ).updateViewState()
            val conn = persistanceUseCase.getWalletConnection()
            return@withContext walletAdapter.transact(sender,
                if (conn is NotConnected) SignInWithSolana.Payload("solana.com",
                    "Sign in to Seeker RPS") else null) {
                block(it)
            }.also { result ->
                when (result) {
                    is TransactionResult.Success -> {
                        val currentConn = Connected(
                            SolanaPublicKey(result.authResult.accounts.first().publicKey),
                            result.authResult.accounts.first().accountLabel ?: "",
                            result.authResult.authToken,
                            result.authResult.walletUriBase
                        )

                        persistanceUseCase.persistConnection(currentConn.publicKey,
                            currentConn.accountLabel, currentConn.authToken, currentConn.walletUriBase)

                        _state.value.copy(
                            userAddress = currentConn.publicKey.base58(),
                            userLabel = currentConn.accountLabel,
                            solBalance = solanaRpcUseCase.getBalance(currentConn.publicKey)
                        ).updateViewState()
                    }
                    is TransactionResult.NoWalletFound -> {
                        _state.value.copy(
                            walletFound = false,
                            error = result.message
                        ).updateViewState()
                    }
                    is TransactionResult.Failure -> {
                        _state.value.copy(
                            error = result.message
                        ).updateViewState()
                    }
                }
                _state.value.copy(
                    isLoading = false,
                ).updateViewState()
            }
        }

}
