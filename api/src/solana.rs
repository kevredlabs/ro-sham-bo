//! Interaction with the rps-escrow program on Solana devnet.
//!
//! Follows [Anchor Rust client](https://www.anchor-lang.com/docs/clients/rust): IDL-generated
//! client to build instructions. Send via RpcClient so the handler stays Send (keypair is !Send).

use anchor_attribute_program::declare_program;
use anchor_client::Client;
use anchor_lang::prelude::Pubkey;
use solana_client::rpc_client::RpcClient;
use solana_sdk::{
    commitment_config::CommitmentConfig,
    signature::{Keypair, Signer},
    transaction::Transaction,
};
use std::rc::Rc;
use std::str::FromStr;
use std::sync::Arc;

use crate::config::Config;

declare_program!(rps_escrow);

use rps_escrow::client::{accounts, args};

/// RPS escrow program id (from IDL). Use for PDA derivation when not using the full client.
pub fn program_id() -> Pubkey {
    rps_escrow::ID
}

/// PDA seeds for game escrow: `["game_escrow", creator, game_id]`.
pub fn game_escrow_pda(program_id: &Pubkey, creator: &Pubkey, game_id: &[u8; 16]) -> Pubkey {
    let seeds: &[&[u8]] = &[b"game_escrow", creator.as_ref(), game_id.as_ref()];
    Pubkey::find_program_address(seeds, program_id).0
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

/// Calls the rps-escrow **resolve** instruction on devnet using the IDL-generated client.
/// Builds the instruction with the client (Anchor doc pattern); sends with RpcClient so the handler stays Send.
/// `game_id` must be the 16-byte UUID (no hyphens). `creator_pubkey` and `winner_pubkey` are base58.
pub fn resolve_game(
    rpc_url: &str,
    authority_keypair: &Keypair,
    game_id: [u8; 16],
    creator_pubkey: &str,
    winner_pubkey: &str,
) -> Result<ResolveResult, String> {
    let creator = Pubkey::from_str(creator_pubkey).map_err(|e| e.to_string())?;
    let winner = Pubkey::from_str(winner_pubkey).map_err(|e| e.to_string())?;

    let commitment = CommitmentConfig::confirmed();
    let client = Client::new_with_options(
        anchor_client::Cluster::Custom(rpc_url.to_string(), rpc_url.to_string()),
        Rc::new(authority_keypair),
        commitment,
    );
    let program = client.program(rps_escrow::ID).map_err(|e| e.to_string())?;

    let game_escrow = game_escrow_pda(&rps_escrow::ID, &creator, &game_id);

    let resolve_ix = program
        .request()
        .accounts(accounts::Resolve {
            authority: authority_keypair.pubkey(),
            game_escrow,
            winner_destination: winner,
        })
        .args(args::Resolve { winner })
        .instructions()
        .map_err(|e| e.to_string())?
        .into_iter()
        .next()
        .ok_or_else(|| "resolve instruction missing".to_string())?;

    let rpc = RpcClient::new_with_commitment(rpc_url.to_string(), commitment);
    let recent_blockhash = rpc.get_latest_blockhash().map_err(|e| e.to_string())?;
    let tx = Transaction::new_signed_with_payer(
        &[resolve_ix],
        Some(&authority_keypair.pubkey()),
        &[authority_keypair],
        recent_blockhash,
    );

    let sig = rpc.send_and_confirm_transaction(&tx).map_err(|e| e.to_string())?;
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
        let resolve_authority = load_keypair(&config.resolve_authority_keypair_path)
            .ok()
            .map(Arc::new);

        if resolve_authority.is_none() {
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
            authority.as_ref(),
            game_id,
            creator_pubkey,
            winner_pubkey,
        )
    }
}
