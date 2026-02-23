//! Program error codes.

use anchor_lang::prelude::*;

#[error_code]
pub enum EscrowError {
    #[msg("Amount is invalid")]
    InvalidAmount,
    #[msg("Game already resolved")]
    AlreadyResolved,
    #[msg("Joiner already set")]
    JoinerAlreadySet,
    #[msg("Winner already set")]
    WinnerAlreadySet,
    #[msg("No joiner has deposited yet")]
    NoJoiner,
    #[msg("Insufficient balance")]
    InsufficientBalance,
    #[msg("Winner must be creator or joiner")]
    InvalidWinner,
    #[msg("Unauthorized: signer is not the resolve authority")]
    Unauthorized,
    #[msg("Unauthorized: the account is not the creator of the game")]
    UnauthorizedCreator,
    #[msg("Unauthorized: the account is not the joiner of the game")]
    UnauthorizedJoiner,
    #[msg("Invalid treasury")]
    InvalidTreasury,
}
