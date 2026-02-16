//! Resolve game: authority closes the escrow; all SOL (full balance) is sent to the winner.

use anchor_lang::prelude::*;

use crate::errors::EscrowError;
use crate::state::GameEscrow;

#[derive(Accounts)]
pub struct Resolve<'info> {
    /// Authority that can resolve (e.g. backend). In production, restrict to a known key.
    pub authority: Signer<'info>,

    #[account(
        mut,
        seeds = [b"game_escrow", game_escrow.creator.as_ref(), game_escrow.game_id.as_ref()],
        bump = game_escrow.bump,
        constraint = !game_escrow.resolved @ EscrowError::AlreadyResolved,
        constraint = game_escrow.joiner.is_some() @ EscrowError::NoJoiner,
        close = winner_destination,
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    /// Receives the full escrow balance (all lamports) when the account is closed.
    /// Must be the winner's system account (winner must be creator or joiner).
    /// CHECK: Validated in instruction: must match winner pubkey and winner must be creator or joiner.
    #[account(mut)]
    pub winner_destination: UncheckedAccount<'info>,
}

impl<'info> Resolve<'info> {
    /// Resolves the game: validates winner, then close sends all SOL to winner_destination.
    pub fn resolve(&mut self, winner: Pubkey) -> Result<()> {
        require!(
            self.authority.key() == crate::RESOLVE_AUTHORITY,
            EscrowError::Unauthorized
        );
        require!(
            winner == self.game_escrow.creator || self.game_escrow.joiner == Some(winner),
            EscrowError::InvalidWinner
        );
        require!(
            self.winner_destination.key() == winner,
            EscrowError::InvalidWinner
        );

        let balance = self.game_escrow.to_account_info().lamports();
        let min_balance = Rent::get()?.minimum_balance(self.game_escrow.to_account_info().data_len());
        require!(balance > min_balance, EscrowError::InsufficientBalance);

        Ok(())
    }
}
