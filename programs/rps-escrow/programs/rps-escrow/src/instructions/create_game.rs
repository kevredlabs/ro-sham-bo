//! Create game escrow: creator deposits `amount` lamports (1 SOL = 10^9 lamports).
//! PDA seeds: ["game_escrow", creator, game_id].

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

use crate::errors::EscrowError;
use crate::state::GameEscrow;

/// Minimum bet per player: 0.001 SOL (also above rent-exempt for vault).
const MIN_BET_LAMPORTS: u64 = 1_000_000;

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

    #[account(
        mut,
        seeds = [b"vault",game_escrow.key().as_ref()],
        bump
    )]
    pub vault: SystemAccount<'info>,

    pub system_program: Program<'info, System>,
}

impl<'info> CreateGame<'info> {
    /// Initializes the game escrow and transfers `amount` lamports from creator to the vault.
    pub fn create_and_deposit(
        &mut self,
        game_id: [u8; 16],
        amount: u64,
        bumps: &CreateGameBumps,
    ) -> Result<()> {
        // amount must be at least MIN_BET_LAMPORTS (0.001 SOL) and above rent-exempt for the vault
        let rent_exempt: u64 = Rent::get()?.minimum_balance(self.vault.to_account_info().data_len());
        require!(amount >= MIN_BET_LAMPORTS, EscrowError::InvalidAmount);
        require!(amount > rent_exempt, EscrowError::InvalidAmount);

        self.game_escrow.set_inner(GameEscrow {
            creator: self.creator.key(),
            game_id,
            joiner: None,
            amount_per_player: amount,
            bump: bumps.game_escrow,
            vault_bump: bumps.vault,
            resolved: false,
            winner: None,
        });

        // transfer the amount from the creator to the vault. It will also create the vault account owned by system program. 
        let cpi_ctx = CpiContext::new(
            self.system_program.to_account_info(),
            Transfer {
                from: self.creator.to_account_info(),
                to: self.vault.to_account_info(),
            },
        );
        transfer(cpi_ctx, amount)?;

        Ok(())
    }
}
