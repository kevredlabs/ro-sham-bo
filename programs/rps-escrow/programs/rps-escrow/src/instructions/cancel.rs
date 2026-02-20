//! Cancel game: the creator gets his money back in case the game was not played and he wants to cancel it before the joiner joins.

use anchor_lang::prelude::*;

use crate::errors::EscrowError;
use crate::state::GameEscrow;

use anchor_lang::system_program::{transfer, Transfer};

#[derive(Accounts)]
pub struct Cancel<'info> {

    /// Creator of the game
    #[account(mut)]
    pub creator: Signer<'info>,

    #[account(
        mut,
        seeds = [b"game_escrow", game_escrow.creator.as_ref(), game_escrow.game_id.as_ref()],
        bump = game_escrow.bump,
        constraint = !game_escrow.resolved @ EscrowError::AlreadyResolved,
        constraint = game_escrow.joiner.is_none() @ EscrowError::JoinerAlreadySet,
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

    pub system_program: Program<'info, System>,
}

impl<'info> Cancel<'info> {
    /// Cancel the game: validates creator, then close sends all SOL to creator.
    /// 
    pub fn cancel(&mut self) -> Result<()> {
        require!(
            self.creator.key() == self.game_escrow.creator,
            EscrowError::UnauthorizedCreator
        );

        let payout = self.game_escrow.amount_per_player;

        require!(self.vault.lamports() == payout, EscrowError::InsufficientBalance);


        let seeds: &[&[&[u8]]] = &[&[
            b"vault", 
            &self.game_escrow.key().to_bytes(),
            &[self.game_escrow.vault_bump]]];
    
    
    
        let cpi_ctx = CpiContext::new_with_signer(
            self.system_program.to_account_info(),
            Transfer {
                from: self.vault.to_account_info(),
                to: self.creator.to_account_info(),
            },
            seeds);
    
        transfer(cpi_ctx, payout)?; // send the full amount to the creator
        
        Ok(()) // close the escrow account and give back rent to the creator
    }
}
