//! Resolve game: authority closes the escrow; all SOL (full balance) is sent to the winner.

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

use crate::errors::EscrowError;
use crate::state::GameEscrow;

#[derive(Accounts)]
pub struct Resolve<'info> {
    /// Authority that can resolve (e.g. backend)
    #[account(mut)]
    pub authority: Signer<'info>,

    #[account(
        mut,
        seeds = [b"game_escrow", game_escrow.creator.as_ref(), game_escrow.game_id.as_ref()],
        bump = game_escrow.bump,
        constraint = !game_escrow.resolved @ EscrowError::AlreadyResolved,
        constraint = game_escrow.joiner.is_some() @ EscrowError::NoJoiner,
        constraint = game_escrow.creator == creator.key() @ EscrowError::UnauthorizedCreator,
        close = creator
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    #[account(
        mut,
        seeds = [b"vault",game_escrow.key().as_ref()],
        bump = game_escrow.vault_bump,
    )]
    pub vault: SystemAccount<'info>,

    /// Must be the winner's system account (winner must be creator or joiner).
    /// CHECK: Validated in instruction: must match winner pubkey and winner must be creator or joiner.
    #[account(mut)]
    pub winner_destination: UncheckedAccount<'info>,

    /// CHECK: Validated in instruction: must match creator pubkey.
    #[account(mut)]
    pub creator: AccountInfo<'info>,

    pub system_program: Program<'info, System>
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

        let payout = self.game_escrow.amount_per_player.checked_mul(2).ok_or(EscrowError::InvalidAmount)?;

        require!(self.vault.lamports() >= payout, EscrowError::InsufficientBalance);

        let seeds: &[&[&[u8]]] = &[&[
        b"vault", 
        &self.game_escrow.key().to_bytes(),
        &[self.game_escrow.vault_bump]]];



        let cpi_ctx = CpiContext::new_with_signer(
            self.system_program.to_account_info(),
            Transfer {
                from: self.vault.to_account_info(),
                to: self.winner_destination.to_account_info(),
            },
            seeds);

        transfer(cpi_ctx, payout)?; // send the full amount to the winner


        Ok(()) // close the escrow account and give back rent to the creator
    }
}
