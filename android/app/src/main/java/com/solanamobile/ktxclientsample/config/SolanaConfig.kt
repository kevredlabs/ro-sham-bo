package com.solanamobile.ktxclientsample.config

import com.solanamobile.ktxclientsample.BuildConfig
import com.solana.publickey.SolanaPublicKey

/**
 * Central config for Solana RPC and rps_escrow program.
 * RPC_URL is set per build flavor: develop = devnet, prod = mainnet (Helius).
 */
object SolanaConfig {

    // --- RPC (from build flavor: develop = devnet, prod = mainnet) ---
    val RPC_URL: String = BuildConfig.SOLANA_RPC_URL

    const val LAMPORTS_PER_SOL: Long = 1_000_000_000

    // --- rps_escrow program (matches api/idls/rps_escrow.json) ---
    val RPS_ESCROW_PROGRAM_ID: SolanaPublicKey =
        SolanaPublicKey.from("F4d4VwBaQrqf5hUZs74XoiVCAo76BpeRSqABxMMzG7kN")

    val SYSTEM_PROGRAM_ID: SolanaPublicKey =
        SolanaPublicKey.from("11111111111111111111111111111111")

    val GAME_ESCROW_SEED: ByteArray = "game_escrow".encodeToByteArray()

    /** Vault PDA seed (vault holds SOL for the game; seeds = [b"vault", game_escrow.key()]). */
    val VAULT_SEED: ByteArray = "vault".encodeToByteArray()

    /** create_game instruction discriminator (from IDL). */
    val CREATE_GAME_DISCRIMINATOR: ByteArray = byteArrayOf(
        124.toByte(), 69.toByte(), 75.toByte(), 66.toByte(),
        184.toByte(), 220.toByte(), 72.toByte(), 206.toByte()
    )

    /** join_game instruction discriminator (from IDL). */
    val JOIN_GAME_DISCRIMINATOR: ByteArray = byteArrayOf(
        107.toByte(), 112.toByte(), 18.toByte(), 38.toByte(),
        56.toByte(), 173.toByte(), 60.toByte(), 128.toByte()
    )

    /** cancel instruction discriminator (sha256("global:cancel")[0..8]). */
    val CANCEL_DISCRIMINATOR: ByteArray = byteArrayOf(
        232.toByte(), 219.toByte(), 223.toByte(), 41.toByte(),
        219.toByte(), 236.toByte(), 220.toByte(), 190.toByte()
    )
}
