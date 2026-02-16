//! Create game escrow: creator deposits `amount` lamports.
//! PDA seeds: ["game_escrow", creator, game_id].

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

use crate::errors::EscrowError;
use crate::state::GameEscrow;

#[derive(Accounts)]
#[instruction(game_id: [u8; 16], amount: u64)]

pub struct CreateGame<'info> {
    #[account(mut)]
    pub creator: Signer<'info>,

    #[account(
        init,
        payer = creator,
        space = 8 + GameEscrow::INIT_SPACE,
        seeds = [b"game_escrow", creator.key().as_ref(), game_id.as_ref()],
        bump
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    pub system_program: Program<'info, System>,
}

impl<'info> CreateGame<'info> {
    /// Initializes the game escrow and transfers `amount` lamports from creator to the PDA.
    pub fn create_and_deposit(
        &mut self,
        game_id: [u8; 16],
        amount: u64,
        bumps: &CreateGameBumps,
    ) -> Result<()> {
        require!(amount > 0, EscrowError::InvalidAmount);

        self.game_escrow.set_inner(GameEscrow {
            creator: self.creator.key(),
            game_id,
            joiner: None,
            amount_per_player: amount,
            bump: bumps.game_escrow,
            resolved: false,
            winner: None,
        });

        let cpi_ctx = CpiContext::new(
            self.system_program.to_account_info(),
            Transfer {
                from: self.creator.to_account_info(),
                to: self.game_escrow.to_account_info(),
            },
        );
        transfer(cpi_ctx, amount)?;

        Ok(())
    }
}
