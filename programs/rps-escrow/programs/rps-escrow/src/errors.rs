//! Program error codes.

use anchor_lang::prelude::*;

#[error_code]
pub enum EscrowError {
    #[msg("Amount must be greater than zero")]
    InvalidAmount,
    #[msg("Game already resolved")]
    AlreadyResolved,
    #[msg("Joiner already set")]
    JoinerAlreadySet,
    #[msg("No joiner has deposited yet")]
    NoJoiner,
    #[msg("Insufficient balance in escrow")]
    InsufficientBalance,
    #[msg("Winner must be creator or joiner")]
    InvalidWinner,
    #[msg("Unauthorized: signer is not the resolve authority")]
    Unauthorized,
}
