//! Refund game: both the creator and the joiner get their money back in case the game was not played.

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

use crate::errors::EscrowError;
use crate::state::GameEscrow;

#[derive(Accounts)]
pub struct Refund<'info> {

    /// Creator of the game
    #[account(mut)]
    pub authority: Signer<'info>,

    #[account(
        mut,
        seeds = [b"game_escrow", game_escrow.creator.as_ref(), game_escrow.game_id.as_ref()],
        bump = game_escrow.bump,
        constraint = !game_escrow.resolved @ EscrowError::AlreadyResolved,
        constraint = game_escrow.joiner.is_some() @ EscrowError::NoJoiner,
        constraint = game_escrow.winner.is_none() @ EscrowError::WinnerAlreadySet,
        close = creator,
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    #[account(
        mut,
        seeds = [b"vault",game_escrow.key().as_ref()],
        bump = game_escrow.vault_bump,
    )]
    pub vault: SystemAccount<'info>,


    /// CHECK: Validated in instruction: must match creator pubkey.
    #[account(mut)]
    pub creator: AccountInfo<'info>,

    /// CHECK: Validated in instruction: must match joiner pubkey.
    #[account(mut)]
    pub joiner: AccountInfo<'info>,

    pub system_program: Program<'info, System>,
}

impl<'info> Refund<'info> {
    /// Refund both the creator and the joiner.
    pub fn refund(&mut self) -> Result<()> {
        require!(
            self.authority.key() == crate::RESOLVE_AUTHORITY,
            EscrowError::Unauthorized
        );

        let joiner_pubkey = self.game_escrow.joiner.ok_or(EscrowError::NoJoiner)?;

        require!(
            joiner_pubkey == self.joiner.key(),
            EscrowError::UnauthorizedJoiner
        );
        require!(
            self.game_escrow.creator == self.creator.key(),
            EscrowError::UnauthorizedCreator
        );

        let payout = self.game_escrow.amount_per_player.checked_mul(2).ok_or(EscrowError::InvalidAmount)?;

        require!(self.vault.lamports() >= payout, EscrowError::InsufficientBalance);


        let seeds: &[&[&[u8]]] = &[&[
            b"vault", 
            &self.game_escrow.key().to_bytes(),
            &[self.game_escrow.vault_bump]]];
    
    
    
        let cpi_ctx_1 = CpiContext::new_with_signer(
    self.system_program.to_account_info(),
   Transfer {
                from: self.vault.to_account_info(),
                to: self.joiner.to_account_info(),
                },
            seeds);
    
            transfer(cpi_ctx_1, self.game_escrow.amount_per_player)?; // send the full amount to the joiner

            
        let cpi_ctx_2 = CpiContext::new_with_signer(
    self.system_program.to_account_info(),
   Transfer {
                from: self.vault.to_account_info(),
                to: self.creator.to_account_info(),
                 },
            seeds);
                
        transfer(cpi_ctx_2, self.game_escrow.amount_per_player)?; // send the full amount to the creator
            
        Ok(()) // close the escrow account and give back rent to the creator
    }
}
