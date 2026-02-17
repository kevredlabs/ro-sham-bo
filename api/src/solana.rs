//! Interaction with the rps-escrow program on Solana devnet.
//!
//! - Derives game escrow PDA and builds the **resolve** instruction.
//! - The API signs resolve with the resolve authority keypair when a game ends.

use sha2::{Digest, Sha256};
use solana_client::rpc_client::RpcClient;
use solana_sdk::{
    commitment_config::CommitmentConfig,
    instruction::{AccountMeta, Instruction},
    pubkey::Pubkey,
    signature::{Keypair, Signer},
    transaction::Transaction,
};
use std::str::FromStr;
use std::sync::Arc;

use crate::config::Config;

/// RPS escrow program id (devnet). Must match `declare_id!` in the program.
pub fn program_id() -> Pubkey {
    Pubkey::from_str("F4d4VwBaQrqf5hUZs74XoiVCAo76BpeRSqABxMMzG7kN").expect("program id")
}

/// Anchor instruction discriminator: first 8 bytes of sha256("global:<name>").
fn instruction_discriminator(name: &str) -> [u8; 8] {
    let preimage = format!("global:{}", name);
    let hash = Sha256::digest(preimage.as_bytes());
    let mut disc = [0u8; 8];
    disc.copy_from_slice(&hash[..8]);
    disc
}

/// PDA seeds for game escrow: `["game_escrow", creator, game_id]`.
pub fn game_escrow_pda(program_id: &Pubkey, creator: &Pubkey, game_id: &[u8; 16]) -> Pubkey {
    let seeds: &[&[u8]] = &[b"game_escrow", creator.as_ref(), game_id.as_ref()];
    Pubkey::find_program_address(seeds, program_id).0
}

/// Builds the **resolve** instruction: authority pays winner and closes escrow.
fn build_resolve_instruction(
    program_id: Pubkey,
    authority: &Pubkey,
    game_escrow: Pubkey,
    winner_destination: Pubkey,
    winner: Pubkey,
) -> Instruction {
    let mut data = instruction_discriminator("resolve").to_vec();
    data.extend_from_slice(winner.as_ref());

    Instruction {
        program_id,
        accounts: vec![
            AccountMeta::new_readonly(*authority, true),
            AccountMeta::new(game_escrow, false),
            AccountMeta::new(winner_destination, false),
        ],
        data,
    }
}

/// Load keypair from a JSON file (array of 64 bytes).
pub fn load_keypair(path: &std::path::Path) -> Result<Keypair, String> {
    let bytes: Vec<u8> = serde_json::from_reader(std::fs::File::open(path).map_err(|e| e.to_string())?)
        .map_err(|e| e.to_string())?;
    Keypair::try_from(bytes.as_slice()).map_err(|e| e.to_string())
}

/// Result of calling resolve on-chain.
#[derive(Debug)]
pub struct ResolveResult {
    pub signature: String,
}

/// Calls the rps-escrow **resolve** instruction on devnet.
/// `game_id` must be the 16-byte UUID (no hyphens). `creator_pubkey` and `winner_pubkey` are base58.
pub fn resolve_game(
    rpc_url: &str,
    program_id: Pubkey,
    authority_keypair: &Keypair,
    game_id: [u8; 16],
    creator_pubkey: &str,
    winner_pubkey: &str,
) -> Result<ResolveResult, String> {
    let creator = Pubkey::from_str(creator_pubkey).map_err(|e| e.to_string())?;
    let winner = Pubkey::from_str(winner_pubkey).map_err(|e| e.to_string())?;

    let client = RpcClient::new_with_commitment(
        rpc_url.to_string(),
        CommitmentConfig::confirmed(),
    );

    let game_escrow = game_escrow_pda(&program_id, &creator, &game_id);

    let ix = build_resolve_instruction(
        program_id,
        &authority_keypair.pubkey(),
        game_escrow,
        winner,
        winner,
    );

    let recent_blockhash = client.get_latest_blockhash().map_err(|e| e.to_string())?;
    let tx = Transaction::new_signed_with_payer(
        &[ix],
        Some(&authority_keypair.pubkey()),
        &[authority_keypair],
        recent_blockhash,
    );

    let sig = client.send_and_confirm_transaction(&tx).map_err(|e| e.to_string())?;
    Ok(ResolveResult {
        signature: sig.to_string(),
    })
}

/// Shared Solana client and config for the app. Resolve is only available if keypair is configured.
#[derive(Clone)]
pub struct SolanaAppClient {
    pub rpc_url: String,
    pub program_id: Pubkey,
    pub resolve_authority: Option<Arc<Keypair>>,
}

impl SolanaAppClient {
    pub fn from_config(config: &Config) -> Self {
        let resolve_authority = config
            .resolve_authority_keypair_path
            .as_ref()
            .and_then(|path| load_keypair(path).ok())
            .map(Arc::new);

        if resolve_authority.is_none() && config.resolve_authority_keypair_path.is_some() {
            log::warn!(
                "RESOLVE_AUTHORITY_KEYPAIR_PATH set but keypair failed to load; on-chain resolve disabled"
            );
        }

        Self {
            rpc_url: config.solana_rpc_url.clone(),
            program_id: config.rps_escrow_program_id,
            resolve_authority,
        }
    }

    /// Returns true if the API can call resolve on the program.
    pub fn can_resolve(&self) -> bool {
        self.resolve_authority.is_some()
    }

    /// Resolve the game on-chain. Fails if keypair not configured or RPC/transaction fails.
    pub fn resolve(
        &self,
        game_id: [u8; 16],
        creator_pubkey: &str,
        winner_pubkey: &str,
    ) -> Result<ResolveResult, String> {
        let authority = self
            .resolve_authority
            .as_ref()
            .ok_or_else(|| "resolve authority keypair not configured".to_string())?;
        resolve_game(
            &self.rpc_url,
            self.program_id,
            authority.as_ref(),
            game_id,
            creator_pubkey,
            winner_pubkey,
        )
    }
}
