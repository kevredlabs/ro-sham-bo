//! RPS game escrow program (Anchor).
//!
//! - **create_game(game_id, amount)**: Creator initializes escrow for one game (game_id = MongoDB _id as 16 bytes, UUID without hyphens) and deposits `amount` SOL.
//! - **join_game**: Second player deposits the same amount into the same PDA.
//! - **resolve(winner)**: Authority resolves the game: all SOL to `winner`, then closes the escrow.
//!
//! PDA seeds: ["game_escrow", creator, game_id] so one creator can have multiple games.

use anchor_lang::prelude::*;



pub mod errors;
pub mod instructions;
pub mod state;

pub use instructions::*;
pub use state::*;
pub use errors::*;


declare_id!("F4d4VwBaQrqf5hUZs74XoiVCAo76BpeRSqABxMMzG7kN");

/// Only this pubkey can call `resolve`. Set to your backend authority keypair's public key.
/// Change by upgrading the program if needed.
pub const RESOLVE_AUTHORITY: Pubkey = pubkey!("GVEseebBBBL1aykkpM2J3opBHkdGjYSdKNEdR68kfQkF");


#[program]
pub mod rps_escrow {
    use super::*;

    /// Creator creates a game escrow and deposits `amount` lamports. `game_id` = 16 bytes (UUID without hyphens, hex).
    pub fn create_game(ctx: Context<CreateGame>, game_id: [u8; 16], amount: u64) -> Result<()> {
        ctx.accounts
            .create_and_deposit(game_id, amount, &ctx.bumps)
    }

    /// Joiner deposits the same amount as the creator into the escrow.
    pub fn join_game(ctx: Context<JoinGame>) -> Result<()> {
        ctx.accounts.deposit_and_join()
    }

    /// Authority resolves the game: full payout to winner, then closes the escrow.
    pub fn resolve(ctx: Context<Resolve>, winner: Pubkey) -> Result<()> {
        ctx.accounts.resolve(winner)
    }

    /// Creator cancels the game: gets his money back in case no game was played and there is no joiner.
    pub fn cancel(ctx: Context<Cancel>) -> Result<()> {
        ctx.accounts.cancel()
    }
}
