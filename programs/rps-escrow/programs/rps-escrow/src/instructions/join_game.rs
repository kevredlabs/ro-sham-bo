//! Join game: second player deposits the same amount into the escrow PDA.

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

use crate::errors::EscrowError;
use crate::state::GameEscrow;

#[derive(Accounts)]
pub struct JoinGame<'info> {
    #[account(mut)]
    pub joiner: Signer<'info>,

    #[account(
        mut,
        seeds = [b"game_escrow", game_escrow.creator.as_ref(), game_escrow.game_id.as_ref()],
        bump = game_escrow.bump,
        constraint = !game_escrow.resolved @ EscrowError::AlreadyResolved,
        constraint = game_escrow.joiner.is_none() @ EscrowError::JoinerAlreadySet,
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    pub system_program: Program<'info, System>,
}

impl<'info> JoinGame<'info> {
    /// Deposits `amount_per_player` lamports from joiner into the escrow and sets joiner on the game.
    pub fn deposit_and_join(&mut self) -> Result<()> {
        let amount = self.game_escrow.amount_per_player;

        let cpi_ctx = CpiContext::new(
            self.system_program.to_account_info(),
            Transfer {
                from: self.joiner.to_account_info(),
                to: self.game_escrow.to_account_info(),
            },
        );
        transfer(cpi_ctx, amount)?;

        self.game_escrow.joiner = Some(self.joiner.key());

        Ok(())
    }
}
