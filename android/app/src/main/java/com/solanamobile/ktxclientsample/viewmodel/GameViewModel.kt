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
import com.solanamobile.ktxclientsample.usecase.SiwsProof
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
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class GameViewState(
    val connectionLoaded: Boolean = false,
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
    /** When true, show RulesScreen. */
    val showRulesScreen: Boolean = false,
    /** When non-null, show CurrentGameScreen (user joined this game). */
    val joinedGameId: String? = null,
    /** Current game screen phase: COUNTDOWN, SELECTION, WAITING_FOR_OTHER, DRAW_NEXT_ROUND, RESULT_COUNTDOWN, RESULT. */
    val gamePhase: String? = null,
    /** Countdown value to display (3, 2, 1). */
    val countdownNumber: Int = 3,
    /** Result message after both played: "rock beats scissors, winner is: pubkey". */
    val gameResultMessage: String? = null,
    /** Whether the current user won (true), lost (false), or draw (null). */
    val isWinner: Boolean? = null,
    /** Stake amount per player in lamports for the current game. */
    val gameAmountPerPlayer: Long = 0L
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
            _state.update {
                it.copy(
                    connectionLoaded = true,
                    userAddress = persistedConn.publicKey.base58(),
                    userLabel = persistedConn.accountLabel
                )
            }
            walletAdapter.authToken = persistedConn.authToken
            viewModelScope.launch {
                val balance = solanaRpcUseCase.getBalance(persistedConn.publicKey)
                _state.update { it.copy(solBalance = balance) }
            }
        } else {
            _state.update { it.copy(connectionLoaded = true) }
        }
    }

    /**
     * Sign in with Solana (SIWS): connect to wallet and verify ownership in one step.
     * Uses walletAdapter.signIn() as per [Solana Mobile docs](https://docs.solanamobile.com/get-started/kotlin/quickstart#sign-in-with-solana-siws).
     * On success, persists connection and SIWS proof for API auth.
     */
    fun signIn(sender: ActivityResultSender) {
        viewModelScope.launch {
            val conn = persistanceUseCase.getWalletConnection()
            if (conn is Connected) {
                // Already connected; no need to sign in again.
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = "") }
            val result = withContext(Dispatchers.Main) {
                walletAdapter.signIn(
                    sender,
                    SignInWithSolana.Payload("solana.com", "Sign in to Seeker RPS")
                )
            }
            when (result) {
                is TransactionResult.Success -> {
                    val currentConn = Connected(
                        SolanaPublicKey(result.authResult.accounts.first().publicKey),
                        result.authResult.accounts.first().accountLabel ?: "",
                        result.authResult.authToken,
                        result.authResult.walletUriBase
                    )
                    persistanceUseCase.persistConnection(
                        currentConn.publicKey,
                        currentConn.accountLabel,
                        currentConn.authToken,
                        currentConn.walletUriBase
                    )
                    result.authResult.signInResult?.let { sr ->
                        val message = String(sr.signedMessage, StandardCharsets.UTF_8)
                        val signature = Base58.encode(sr.signature)
                        persistanceUseCase.persistSiwsProof(message, signature)
                        Log.d(TAG, "signIn: SIWS proof persisted")
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            userAddress = currentConn.publicKey.base58(),
                            userLabel = currentConn.accountLabel,
                            solBalance = solanaRpcUseCase.getBalance(currentConn.publicKey),
                            error = ""
                        )
                    }
                }
                is TransactionResult.NoWalletFound -> {
                    _state.update {
                        it.copy(isLoading = false, walletFound = false, error = result.message)
                    }
                }
                is TransactionResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
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
     * Creates a new game: 1) generate UUID client-side, 2) build & sign on-chain create_game tx,
     * 3) on tx success call API to persist game in DB and get the PIN.
     */
    fun startNewGame(sender: ActivityResultSender, amountPerPlayer: Long) {
        val address = _state.value.userAddress
        if (address.isEmpty()) {
            Log.w(TAG, "startNewGame: no wallet connected")
            _state.update { it.copy(error = "Connect wallet first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = "") }

            val gameId = java.util.UUID.randomUUID().toString()
            Log.d(TAG, "startNewGame: generated gameId=$gameId for $address")

            val gameIdBytes = escrowUseCase.gameIdToBytes(gameId)
            if (gameIdBytes == null) {
                Log.e(TAG, "startNewGame: invalid generated game_id")
                _state.update { it.copy(isLoading = false, error = "Invalid game id") }
                return@launch
            }
            val creator = SolanaPublicKey.from(address)
            val createGameIx = escrowUseCase.buildCreateGameInstruction(creator, gameIdBytes, amountPerPlayer)
            if (createGameIx == null) {
                Log.e(TAG, "startNewGame: buildCreateGameInstruction returned null")
                _state.update { it.copy(isLoading = false, error = "Failed to build create_game instruction") }
                return@launch
            }
            Log.d(TAG, "startNewGame: instruction built, opening wallet session")
            try {
                val txResult = connect(sender) { authResult ->
                    withContext(Dispatchers.IO) {
                        val blockHash = solanaRpcUseCase.getLatestBlockHash()
                        Log.d(TAG, "startNewGame: blockhash=$blockHash, building transaction")
                        val createGameMessage = Message.Builder()
                            .addInstruction(createGameIx)
                            .setRecentBlockhash(blockHash)
                            .build()
                        val unsignedCreateGameTx = Transaction(createGameMessage)
                        Log.d(TAG, "startNewGame: sending transaction to wallet")
                        signAndSendTransactions(arrayOf(unsignedCreateGameTx.serialize()))
                    }
                }
                Log.d(TAG, "startNewGame: connect returned result=${txResult::class.simpleName}" + (if (txResult is TransactionResult.Failure) " message=${txResult.message}" else ""))
                when (txResult) {
                    is TransactionResult.Success -> {
                        val txSignatureBytes = txResult.successPayload?.signatures?.first()
                        txSignatureBytes?.let {
                            Log.i(TAG, "startNewGame: tx success signature=${Base58.encode(it)}")
                            val siwsProof = persistanceUseCase.getSiwsProof()
                            if (siwsProof == null) {
                                _state.update {
                                    it.copy(isLoading = false, error = "Sign in required. Please disconnect and connect again.")
                                }
                                return@launch
                            }
                            Log.d(TAG, "API createGame with SIWS auth")
                            val apiResult = gameApiUseCase.createGame(address, gameId, amountPerPlayer, siwsProof)
                            apiResult.onFailure { e ->
                                Log.e(TAG, "startNewGame: API create failed (on-chain already done)", e)
                                _state.update {
                                    it.copy(isLoading = false, error = "On-chain OK but API failed: ${e.message}")
                                }
                                return@launch
                            }
                            val result = apiResult.getOrNull() ?: return@launch
                            Log.d(TAG, "startNewGame: API ok pin=${result.pin}")
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showNewGameConfigScreen = false,
                                    gamePin = result.pin,
                                    gameId = gameId,
                                    gameAmountPerPlayer = amountPerPlayer,
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

    /**
     * Cancels the current game on-chain: builds the cancel instruction, signs and sends the tx.
     * On success, navigates back to main menu and refunds the creator.
     */
    fun cancelGame(sender: ActivityResultSender) {
        val address = _state.value.userAddress
        val gameId = _state.value.gameId
        if (address.isEmpty() || gameId == null) {
            Log.w(TAG, "cancelGame: no wallet or gameId")
            _state.update { it.copy(error = "Cannot cancel: missing data") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "cancelGame: cancelling gameId=$gameId creator=$address")
            _state.update { it.copy(isLoading = true, error = "") }
            val gameIdBytes = escrowUseCase.gameIdToBytes(gameId)
            if (gameIdBytes == null) {
                Log.e(TAG, "cancelGame: invalid game_id")
                _state.update { it.copy(isLoading = false, error = "Invalid game id") }
                return@launch
            }
            val creator = SolanaPublicKey.from(address)
            val cancelIx = escrowUseCase.buildCancelGameInstruction(creator, gameIdBytes)
            if (cancelIx == null) {
                Log.e(TAG, "cancelGame: buildCancelGameInstruction returned null")
                _state.update { it.copy(isLoading = false, error = "Failed to build cancel instruction") }
                return@launch
            }
            try {
                val txResult = connect(sender) { authResult ->
                    withContext(Dispatchers.IO) {
                        val blockHash = solanaRpcUseCase.getLatestBlockHash()
                        val message = Message.Builder()
                            .addInstruction(cancelIx)
                            .setRecentBlockhash(blockHash)
                            .build()
                        val unsignedTx = Transaction(message)
                        Log.d(TAG, "cancelGame: sending cancel tx to wallet")
                        signAndSendTransactions(arrayOf(unsignedTx.serialize()))
                    }
                }
                when (txResult) {
                    is TransactionResult.Success -> {
                        val sig = txResult.successPayload?.signatures?.first()
                        sig?.let {
                            Log.i(TAG, "cancelGame: tx success signature=${Base58.encode(it)}")
                            val siwsProof = persistanceUseCase.getSiwsProof()
                            if (siwsProof == null) {
                                _state.update { s ->
                                    s.copy(isLoading = false, error = "Sign in required. Please disconnect and connect again.")
                                }
                                return@launch
                            }
                            Log.d(TAG, "API cancelGame with SIWS auth")
                            gameApiUseCase.cancelGame(gameId, address, siwsProof)
                                .onSuccess {
                                    Log.i(TAG, "cancelGame: API cancel success")
                                }
                                .onFailure { e ->
                                    Log.w(TAG, "cancelGame: API cancel failed (on-chain already done)", e)
                                }
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    gamePin = null,
                                    gameId = null,
                                    error = ""
                                )
                            }
                        } ?: run {
                            Log.w(TAG, "cancelGame: Success but no signature")
                            _state.update { it.copy(isLoading = false, error = "Cancel sent but no signature") }
                        }
                    }
                    is TransactionResult.NoWalletFound -> {
                        Log.w(TAG, "cancelGame: NoWalletFound ${txResult.message}")
                        _state.update { it.copy(isLoading = false, walletFound = false, error = txResult.message) }
                    }
                    is TransactionResult.Failure -> {
                        Log.e(TAG, "cancelGame: Failure message=${txResult.message}")
                        _state.update { it.copy(isLoading = false, error = txResult.message) }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "cancelGame: exception", e)
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Cancel failed: ${e.javaClass.simpleName}")
                }
            }
        }
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

    /** Navigate to RulesScreen. */
    fun enterRules() {
        _state.update { it.copy(showRulesScreen = true) }
    }

    /** Returns from RulesScreen to main menu. */
    fun backFromRules() {
        _state.update { it.copy(showRulesScreen = false) }
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
     * Joins a game with the given 4-digit PIN: 1) lookup game by PIN (read-only),
     * 2) build & sign on-chain join_game tx, 3) on tx success call API to persist joiner in DB.
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
            _state.update { it.copy(isLoading = true, error = "") }

            Log.d(TAG, "joinGame: looking up game by PIN")
            val lookupResult = gameApiUseCase.lookupGameByPin(trimmedPin)
            lookupResult.onFailure { e ->
                Log.e(TAG, "joinGame: lookup failed", e)
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "No game found for this PIN")
                }
                return@launch
            }
            val gameState = lookupResult.getOrNull() ?: return@launch
            val gameId = gameState.gameId
            val creatorPubkey = gameState.creatorPubkey
            if (creatorPubkey.isNullOrEmpty()) {
                Log.e(TAG, "joinGame: missing creator_pubkey in game")
                _state.update {
                    it.copy(isLoading = false, error = "Invalid game data (missing creator)")
                }
                return@launch
            }
            Log.d(TAG, "joinGame: found gameId=$gameId creator=$creatorPubkey")

            val gameIdBytes = escrowUseCase.gameIdToBytes(gameId)
            if (gameIdBytes == null) {
                Log.e(TAG, "joinGame: invalid game_id")
                _state.update { it.copy(isLoading = false, error = "Invalid game id") }
                return@launch
            }
            val joiner = SolanaPublicKey.from(address)
            val creator = SolanaPublicKey.from(creatorPubkey)
            val joinGameIx = escrowUseCase.buildJoinGameInstruction(joiner, creator, gameIdBytes)
            if (joinGameIx == null) {
                Log.e(TAG, "joinGame: buildJoinGameInstruction returned null")
                _state.update { it.copy(isLoading = false, error = "Failed to build join_game instruction") }
                return@launch
            }
            Log.d(TAG, "joinGame: instruction built, opening wallet session")
            try {
                val txResult = connect(sender) { authResult ->
                    withContext(Dispatchers.IO) {
                        val blockHash = solanaRpcUseCase.getLatestBlockHash()
                        Log.d(TAG, "joinGame: blockhash=$blockHash, building transaction")
                        val message = Message.Builder()
                            .addInstruction(joinGameIx)
                            .setRecentBlockhash(blockHash)
                            .build()
                        val unsignedTx = Transaction(message)
                        Log.d(TAG, "joinGame: sending transaction to wallet")
                        signAndSendTransactions(arrayOf(unsignedTx.serialize()))
                    }
                }
                Log.d(TAG, "joinGame: connect returned result=${txResult::class.simpleName}" + (if (txResult is TransactionResult.Failure) " message=${txResult.message}" else ""))
                when (txResult) {
                    is TransactionResult.Success -> {
                        val txSignatureBytes = txResult.successPayload?.signatures?.first()
                        txSignatureBytes?.let {
                            Log.i(TAG, "joinGame: tx success signature=${Base58.encode(it)}")
                            val siwsProof = persistanceUseCase.getSiwsProof()
                            if (siwsProof == null) {
                                _state.update {
                                    it.copy(isLoading = false, error = "Sign in required. Please disconnect and connect again.")
                                }
                                return@launch
                            }
                            Log.d(TAG, "API joinGame with SIWS auth")
                            val apiResult = gameApiUseCase.joinGame(trimmedPin, address, siwsProof)
                            apiResult.onFailure { e ->
                                Log.e(TAG, "joinGame: API join failed (on-chain already done)", e)
                                _state.update {
                                    it.copy(isLoading = false, error = "On-chain OK but API failed: ${e.message}")
                                }
                                return@launch
                            }
                            Log.d(TAG, "joinGame: API join ok")
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showJoinGameScreen = false,
                                    gamePin = null,
                                    joinedGameId = gameId,
                                    gamePhase = "COUNTDOWN",
                                    countdownNumber = 3,
                                    gameAmountPerPlayer = gameState.amountPerPlayer,
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
                gameResultMessage = null,
                isWinner = null,
                gameAmountPerPlayer = 0L
            )
        }
    }

    /** Called when CurrentGameScreen is shown. Starts initial 3-2-1 countdown then SELECTION. */
    fun onCurrentGameScreenVisible(gameId: String) {
        val current = _state.value
        // Skip if we're already past countdown (e.g. SELECTION, RESULT). When joining we already set COUNTDOWN so run the loop.
        if (current.gamePhase != null && current.gamePhase != "COUNTDOWN") return
        if (current.gamePhase != "COUNTDOWN") {
            _state.update { it.copy(gamePhase = "COUNTDOWN", countdownNumber = 3) }
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
        val siwsProof = persistanceUseCase.getSiwsProof()
        if (siwsProof == null) {
            _state.update { it.copy(error = "Sign in required. Please disconnect and connect again.") }
            return
        }
        val normalized = choice.lowercase()
        if (normalized !in listOf("rock", "paper", "scissors")) return
        viewModelScope.launch {
            _state.update { it.copy(gamePhase = "WAITING_FOR_OTHER", error = "") }
            Log.d(TAG, "API submitChoice with SIWS auth")
            gameApiUseCase.submitChoice(gameId, pubkey, normalized, siwsProof)
                .onSuccess { gameState ->
                    if (gameState.roundClearedForDraw) {
                        startDrawWithCountdown(gameId, "Draw")
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

    /** Shows 3-2-1 countdown then Draw result, then after 2s goes back to SELECTION. */
    private fun startDrawWithCountdown(gameId: String, drawMessage: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    gamePhase = "RESULT_COUNTDOWN",
                    countdownNumber = 3,
                    gameResultMessage = drawMessage
                )
            }
            for (n in listOf(3, 2, 1)) {
                if (_state.value.joinedGameId != gameId) return@launch
                _state.update { it.copy(countdownNumber = n) }
                delay(1000L)
            }
            if (_state.value.joinedGameId != gameId) return@launch
            _state.update {
                it.copy(gamePhase = "RESULT", countdownNumber = 0)
            }
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
                            startDrawWithCountdown(gameId, "Draw")
                            return@launch
                        }
                        val bothChosen = gameState.creatorChoice != null && gameState.joinerChoice != null
                        if (bothChosen) {
                            if (gameState.winnerPubkey != null && gameState.status == "finished") {
                                val result = buildGameResult(gameState)
                                _state.update {
                                    it.copy(
                                        gamePhase = "RESULT_COUNTDOWN",
                                        countdownNumber = 3,
                                        gameResultMessage = result.message,
                                        isWinner = result.isWinner
                                    )
                                }
                                startResultCountdown(gameId)
                                return@launch
                            }
                            val drawResult = buildGameResult(gameState)
                            startDrawWithCountdown(gameId, drawResult.message)
                            return@launch
                        }
                    }
            }
        }
    }

    private data class GameResult(val message: String, val isWinner: Boolean?)

    private fun buildGameResult(gameState: com.solanamobile.ktxclientsample.usecase.GameState): GameResult {
        val winner = gameState.winnerPubkey
        val c = gameState.creatorChoice ?: ""
        val j = gameState.joinerChoice ?: ""
        val myAddress = _state.value.userAddress
        return when {
            winner == null -> GameResult("Draw: $c vs $j", null)
            else -> {
                val isWin = winner == myAddress
                val msg = if (winner == gameState.creatorPubkey) "$c beats $j" else "$j beats $c"
                GameResult(msg, isWin)
            }
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
                    showRulesScreen = false,
                    joinedGameId = null,
                    gamePhase = null,
                    countdownNumber = 3,
                    gameResultMessage = null,
                    isWinner = null,
                    gameAmountPerPlayer = 0L
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

                        result.authResult.signInResult?.let { sr ->
                            val message = String(sr.signedMessage, StandardCharsets.UTF_8)
                            val signature = Base58.encode(sr.signature)
                            persistanceUseCase.persistSiwsProof(message, signature)
                            Log.d(TAG, "connect: SIWS proof persisted")
                        }

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
