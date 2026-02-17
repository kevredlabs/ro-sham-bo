package com.solanamobile.ktxclientsample.usecase

import com.solanamobile.ktxclientsample.config.SolanaConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.TransactionOptions
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SolanaRpcUseCase @Inject constructor() {

    private val rpc: SolanaRpcClient

    init {
        rpc = SolanaRpcClient(SolanaConfig.RPC_URL, KtorNetworkDriver())
    }

    suspend fun awaitConfirmationAsync(signature: String): Deferred<Boolean> {
        return coroutineScope {
            async {
                return@async withContext(Dispatchers.IO) {
                    rpc.confirmTransaction(signature, TransactionOptions(commitment = Commitment.CONFIRMED))
                        .getOrDefault(false)
                }
            }
        }
    }

    suspend fun getBalance(pubkey: SolanaPublicKey, asReadable: Boolean = true): Double =
        withContext(Dispatchers.IO) {
            val result = rpc.getBalance(pubkey).let { result ->
                result.result ?: throw Error(result.error?.message)
            }

            if (asReadable) {
                result.toDouble() / SolanaConfig.LAMPORTS_PER_SOL.toDouble()
            } else {
                result.toDouble()
            }
        }

    suspend fun getLatestBlockHash(): String =
        withContext(Dispatchers.IO) {
            rpc.getLatestBlockhash().run {
                result?.blockhash ?: throw Error(error?.message)
            }
        }

    companion object {
        const val LAMPORTS_PER_SOL: Long = SolanaConfig.LAMPORTS_PER_SOL
    }
}