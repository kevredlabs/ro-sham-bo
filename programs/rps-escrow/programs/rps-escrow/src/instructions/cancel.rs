//! Cancel game: the creator gets his money back in case the game was not played and he wants to cancel it before the joiner joins.

use anchor_lang::prelude::*;

use crate::errors::EscrowError;
use crate::state::GameEscrow;

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
}

impl<'info> Cancel<'info> {
    /// Cancel the game: validates creator, then close sends all SOL to creator.
    pub fn cancel(&mut self) -> Result<()> {
        require!(
            self.creator.key() == self.game_escrow.creator,
            EscrowError::UnauthorizedCreator
        );
        Ok(())
    }
}
