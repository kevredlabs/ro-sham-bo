//! RPS game escrow program (Anchor).
//!
//! - **create_game(game_id, amount)**: Creator initializes escrow for one game (game_id = MongoDB _id as 16 bytes) and deposits `amount` SOL.
//! - **join_game**: Second player deposits the same amount into the same PDA.
//! - **resolve(winner)**: Authority resolves the game: all SOL to `winner`, or 50/50 on tie.
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


#[program]
pub mod rps_escrow {
    use super::*;

    /// Creator creates a game escrow and deposits `amount` lamports. `game_id` = MongoDB _id as 16 bytes (UUID without hyphens).
    pub fn create_game(ctx: Context<CreateGame>, game_id: [u8; 16], amount: u64) -> Result<()> {
        ctx.accounts
            .create_and_deposit(game_id, amount, &ctx.bumps)
    }

    /// Joiner deposits the same amount as the creator into the escrow.
    pub fn join_game(ctx: Context<JoinGame>) -> Result<()> {
        ctx.accounts.deposit_and_join()
    }

    /// Authority resolves the game: full payout to winner, or 50/50 on tie.
    pub fn resolve(ctx: Context<Resolve>, winner: Option<Pubkey>) -> Result<()> {
        ctx.accounts.resolve(winner)
    }
}
