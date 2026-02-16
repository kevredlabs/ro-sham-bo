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
     * @return Result with CreateGameResult or error message
     */
    suspend fun createGame(creatorWallet: String): Result<CreateGameResult> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("creator_pubkey", creatorWallet)
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
}

data class CreateGameResult(
    val gameId: String,
    val pin: String
)

data class JoinGameResult(
    val gameId: String
)
