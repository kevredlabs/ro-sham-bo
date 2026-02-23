package com.solanamobile.ktxclientsample.usecase

import com.solanamobile.ktxclientsample.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Calls the seeker-rps API to create a game and (later) join by PIN.
 */
class GameApiUseCase @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    /**
     * Creates a new game on the API. Returns game id and 4-digit PIN to share.
     * @param creatorWallet Solana public key (base58) of the game creator
     * @param gameId Pre-generated UUID to use as the game ID (for on-chain-first flow)
     * @return Result with CreateGameResult or error message
     */
    suspend fun createGame(creatorWallet: String, gameId: String? = null, amountPerPlayer: Long = 0L): Result<CreateGameResult> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("creator_pubkey", creatorWallet)
            if (gameId != null) put("game_id", gameId)
            put("amount_per_player", amountPerPlayer)
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl/games/create")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@runCatching Result.failure<CreateGameResult>(
                        Exception("HTTP ${response.code}: $responseBody")
                    )
                }
                val json = JSONObject(responseBody)
                Result.success(
                    CreateGameResult(
                        gameId = json.getString("game_id"),
                        pin = json.getString("pin")
                    )
                )
            }
        }.getOrElse { e -> Result.failure(e) }
    }

    /**
     * Looks up a waiting game by PIN without modifying it (read-only).
     * Returns game info including gameId and creatorPubkey.
     */
    suspend fun lookupGameByPin(pin: String): Result<GameState> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/games/lookup/$pin")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("error", responseBody).ifEmpty { "HTTP ${response.code}" }
                    } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
                    return@runCatching Result.failure<GameState>(Exception(message))
                }
                val json = JSONObject(responseBody)
                Result.success(
                    GameState(
                        gameId = json.getString("_id"),
                        status = json.optString("status", "waiting"),
                        joinerPubkey = null,
                        creatorPubkey = json.optString("creator_pubkey", null).takeIf { it.isNotEmpty() },
                        amountPerPlayer = json.optLong("amount_per_player", 0L)
                    )
                )
            }
        }.getOrElse { e -> Result.failure(e) }
    }

    /**
     * Joins an existing game by 4-digit PIN. Returns game id on success.
     * @param pin 4-digit PIN shared by the game creator
     * @param joinerWallet Solana public key (base58) of the player joining
     * @return Result with JoinGameResult or error message
     */
    suspend fun joinGame(pin: String, joinerWallet: String): Result<JoinGameResult> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("pin", pin)
            put("joiner_pubkey", joinerWallet)
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl/games/join")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("error", responseBody).ifEmpty { "HTTP ${response.code}" }
                    } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
                    return@runCatching Result.failure<JoinGameResult>(Exception(message))
                }
                val json = JSONObject(responseBody)
                Result.success(
                    JoinGameResult(gameId = json.getString("game_id"))
                )
            }
        }.getOrElse { e -> Result.failure(e) }
    }

    /**
     * Fetches game state by ID (for polling). Returns status and joiner_pubkey.
     */
    suspend fun getGame(gameId: String): Result<GameState> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/games/$gameId")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("error", responseBody).ifEmpty { "HTTP ${response.code}" }
                    } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
                    return@runCatching Result.failure<GameState>(Exception(message))
                }
                val json = JSONObject(responseBody)
                val status = json.optString("status", "waiting")
                val joinerPubkey = if (json.has("joiner_pubkey") && !json.isNull("joiner_pubkey")) {
                    json.optString("joiner_pubkey", null)
                } else null
                val creatorChoice = if (json.has("creator_choice") && !json.isNull("creator_choice")) {
                    json.optString("creator_choice", null)
                } else null
                val joinerChoice = if (json.has("joiner_choice") && !json.isNull("joiner_choice")) {
                    json.optString("joiner_choice", null)
                } else null
                val winnerPubkey = if (json.has("winner_pubkey") && !json.isNull("winner_pubkey")) {
                    json.optString("winner_pubkey", null)
                } else null
                val roundClearedForDraw = json.optBoolean("round_cleared_for_draw", false)
                Result.success(
                    GameState(
                        gameId = json.optString("_id", gameId),
                        status = status,
                        joinerPubkey = joinerPubkey,
                        creatorChoice = creatorChoice,
                        joinerChoice = joinerChoice,
                        winnerPubkey = winnerPubkey,
                        creatorPubkey = json.optString("creator_pubkey", null).takeIf { it.isNotEmpty() },
                        roundClearedForDraw = roundClearedForDraw,
                        amountPerPlayer = json.optLong("amount_per_player", 0L)
                    )
                )
            }
        }.getOrElse { e -> Result.failure(e) }
    }

    /**
     * Submits the current player's choice (rock, paper, or scissors) for the game.
     */
    suspend fun submitChoice(gameId: String, pubkey: String, choice: String): Result<GameState> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("pubkey", pubkey)
            put("choice", choice.lowercase())
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl/games/$gameId/choice")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("error", responseBody).ifEmpty { "HTTP ${response.code}" }
                    } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
                    return@runCatching Result.failure<GameState>(Exception(message))
                }
                val json = JSONObject(responseBody)
                val status = json.optString("status", "waiting")
                val joinerPubkey = if (json.has("joiner_pubkey") && !json.isNull("joiner_pubkey")) {
                    json.optString("joiner_pubkey", null)
                } else null
                val creatorChoice = if (json.has("creator_choice") && !json.isNull("creator_choice")) {
                    json.optString("creator_choice", null)
                } else null
                val joinerChoice = if (json.has("joiner_choice") && !json.isNull("joiner_choice")) {
                    json.optString("joiner_choice", null)
                } else null
                val winnerPubkey = if (json.has("winner_pubkey") && !json.isNull("winner_pubkey")) {
                    json.optString("winner_pubkey", null)
                } else null
                val roundClearedForDraw = json.optBoolean("round_cleared_for_draw", false)
                Result.success(
                    GameState(
                        gameId = json.optString("_id", gameId),
                        status = status,
                        joinerPubkey = joinerPubkey,
                        creatorChoice = creatorChoice,
                        joinerChoice = joinerChoice,
                        winnerPubkey = winnerPubkey,
                        creatorPubkey = json.optString("creator_pubkey", null).takeIf { it.isNotEmpty() },
                        roundClearedForDraw = roundClearedForDraw,
                        amountPerPlayer = json.optLong("amount_per_player", 0L)
                    )
                )
            }
        }.getOrElse { e -> Result.failure(e) }
    }
    /**
     * Cancels a game on the API (sets status to "cancelled").
     * Should be called after the on-chain cancel tx succeeds.
     */
    suspend fun cancelGame(gameId: String, creatorPubkey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("creator_pubkey", creatorPubkey)
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl/games/$gameId/cancel")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("error", responseBody).ifEmpty { "HTTP ${response.code}" }
                    } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
                    return@runCatching Result.failure<Unit>(Exception(message))
                }
                Result.success(Unit)
            }
        }.getOrElse { e -> Result.failure(e) }
    }
}

data class CreateGameResult(
    val gameId: String,
    val pin: String
)

data class JoinGameResult(
    val gameId: String
)

data class GameState(
    val gameId: String,
    val status: String,
    val joinerPubkey: String?,
    val creatorChoice: String? = null,
    val joinerChoice: String? = null,
    val winnerPubkey: String? = null,
    val creatorPubkey: String? = null,
    val roundClearedForDraw: Boolean = false,
    val amountPerPlayer: Long = 0L
)
