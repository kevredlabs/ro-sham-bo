//! Game escrow account state.

use anchor_lang::prelude::*;

#[account]
#[derive(InitSpace)]
pub struct GameEscrow {
    pub creator: Pubkey,
    /// MongoDB game _id as 16 bytes (UUID without hyphens and 32 hex chars). Matches DB field _id.
    pub game_id: [u8; 16],
    pub joiner: Option<Pubkey>,
    pub amount_per_player: u64,
    pub bump: u8,
    pub resolved: bool,
    pub winner: Option<Pubkey>,
}
