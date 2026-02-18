package com.solanamobile.ktxclientsample.usecase

import android.util.Log
import com.solanamobile.ktxclientsample.config.SolanaConfig
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import javax.inject.Inject

/**
 * Builds instructions for the rps_escrow program (create_game, join_game).
 * Program ID and instruction layout match the Anchor IDL in api/idls/rps_escrow.json.
 * Constants come from [SolanaConfig].
 */
class EscrowUseCase @Inject constructor() {

    /**
     * Builds the create_game instruction for the rps_escrow program.
     * Creator deposits [amountLamports] into the game escrow PDA.
     * Invalid amount (<= 0) is rejected on-chain by the program.
     *
     * @param creator Creator's public key (signer and fee payer).
     * @param gameIdBytes 16 bytes (UUID without hyphens, same as API game_id).
     * @param amountLamports Amount to deposit in lamports.
     * @return TransactionInstruction for create_game, or null if PDA derivation fails.
     */
    suspend fun buildCreateGameInstruction(
        creator: SolanaPublicKey,
        gameIdBytes: ByteArray,
        amountLamports: Long
    ): TransactionInstruction? {
        Log.d(TAG, "buildCreateGameInstruction: creator=${creator.base58()} gameIdBytes.size=${gameIdBytes.size} amountLamports=$amountLamports")
        try {
            require(gameIdBytes.size == 16) { "game_id must be 16 bytes" }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "buildCreateGameInstruction: validation failed", e)
            throw e
        }

        val seeds = listOf(
            SolanaConfig.GAME_ESCROW_SEED,
            creator.bytes,
            gameIdBytes
        )
        Log.d(TAG, "buildCreateGameInstruction: deriving PDA programId=${SolanaConfig.RPS_ESCROW_PROGRAM_ID.base58()}")
        val pdaResult = ProgramDerivedAddress.find(seeds, SolanaConfig.RPS_ESCROW_PROGRAM_ID)
        val gameEscrowPda = pdaResult.getOrNull()
        if (gameEscrowPda == null) {
            Log.e(TAG, "buildCreateGameInstruction: PDA derivation failed (getOrNull() is null)")
            return null
        }
        Log.d(TAG, "buildCreateGameInstruction: game_escrow PDA=${SolanaPublicKey(gameEscrowPda.bytes).base58()}")

        // Accounts: creator (signer, writable), game_escrow (writable), system_program (readonly)
        val accounts = listOf(
            AccountMeta(creator, true, true),
            AccountMeta(SolanaPublicKey(gameEscrowPda.bytes), false, true),
            AccountMeta(SolanaConfig.SYSTEM_PROGRAM_ID, false, false)
        )

        // Instruction data: 8-byte Anchor discriminator (create_game) + game_id (16) + amount (u64 LE)
        // Layout: [0..7] discriminator, [8..23] game_id, [24..31] amount
        val data = ByteArray(8 + 16 + 8).apply {
            SolanaConfig.CREATE_GAME_DISCRIMINATOR.copyInto(this, 0)
            gameIdBytes.copyInto(this, 8)
            for (i in 0..7) this[24 + i] = ((amountLamports shr (i * 8)) and 0xFFL).toByte()
        }
        Log.d(TAG, "buildCreateGameInstruction: instruction data size=${data.size} discriminator=${data.take(8).joinToString("") { "%02x".format(it) }} amount=$amountLamports")

        val instruction = TransactionInstruction(
            SolanaConfig.RPS_ESCROW_PROGRAM_ID,
            accounts,
            data
        )
        Log.d(TAG, "buildCreateGameInstruction: success")
        return instruction
    }

    private companion object {
        const val TAG = "SeekerRPS"
    }

    /**
     * Converts API game_id (UUID string with hyphens) to 16 bytes for the program.
     * Example: "550e8400-e29b-41d4-a716-446655440000" -> 16 bytes (hex decoded).
     */
    fun gameIdToBytes(gameIdUuid: String): ByteArray? {
        val hex = gameIdUuid.replace("-", "")
        if (hex.length != 32) return null
        return ByteArray(16) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
