//! Configuration loaded from environment variables.
//! Single place to validate and document required env vars.
//!
//! **Solana (devnet):**
//! - `SOLANA_RPC_URL` — RPC endpoint (default: https://api.devnet.solana.com)
//! - `RPS_ESCROW_PROGRAM_ID` — RPS escrow program id (required)
//! - `RESOLVE_AUTHORITY_KEYPAIR_PATH` — path to JSON keypair file for resolve authority (required)

pub struct Config {
    pub mongodb_uri: String,
    pub db_name: String,
    /// Solana RPC URL (e.g. https://api.devnet.solana.com).
    pub solana_rpc_url: String,
    /// RPS escrow program id (devnet deployed).
    pub rps_escrow_program_id: solana_sdk::pubkey::Pubkey,
    /// Path to resolve authority keypair JSON file.
    pub resolve_authority_keypair_path: std::path::PathBuf,
}

impl Config {
    /// Load config from environment. Panics if required vars are missing.
    pub fn from_env() -> Self {
        let solana_rpc_url = std::env::var("SOLANA_RPC_URL")
            .unwrap_or_else(|_| "https://api.devnet.solana.com".to_string());
        let program_id_str = std::env::var("RPS_ESCROW_PROGRAM_ID")
            .expect("RPS_ESCROW_PROGRAM_ID must be set");
        let program_id = program_id_str
            .parse()
            .expect("RPS_ESCROW_PROGRAM_ID must be a valid base58 pubkey");
        let resolve_authority_keypair_path = std::env::var("RESOLVE_AUTHORITY_KEYPAIR_PATH")
            .expect("RESOLVE_AUTHORITY_KEYPAIR_PATH must be set")
            .into();

        Self {
            mongodb_uri: std::env::var("MONGODB_URI").expect("MONGODB_URI must be set"),
            db_name: std::env::var("MONGODB_DB_NAME").expect("MONGODB_DB_NAME must be set"),
            solana_rpc_url,
            rps_escrow_program_id: program_id,
            resolve_authority_keypair_path,
        }
    }
}
