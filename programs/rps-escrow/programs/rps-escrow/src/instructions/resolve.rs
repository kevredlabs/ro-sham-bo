//! Resolve game: authority pays out SOL to winner (full) or 50/50 on tie.

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

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
        constraint = game_escrow.joiner.is_some() @ EscrowError::NoJoiner,
    )]
    pub game_escrow: Account<'info, GameEscrow>,

    #[account(mut)]
    pub creator: SystemAccount<'info>,

    #[account(mut)]
    pub joiner: SystemAccount<'info>,

    /// Receives full payout when there is a winner. Must be the winner's system account.
    /// CHECK: Validated in instruction: must be creator or joiner when winner is Some.
    #[account(mut)]
    pub winner_destination: UncheckedAccount<'info>,

    pub system_program: Program<'info, System>,
}

impl<'info> Resolve<'info> {
    /// Resolves the game: full payout to winner, or 50/50 to creator and joiner on tie.
    pub fn resolve(&mut self, winner: Option<Pubkey>) -> Result<()> {
        require!(!self.game_escrow.resolved, EscrowError::AlreadyResolved);
        require!(
            winner.is_none()
                || winner == Some(self.game_escrow.creator)
                || self.game_escrow.joiner == winner,
            EscrowError::InvalidWinner
        );

        if let Some(winner_pubkey) = winner {
            require!(
                self.winner_destination.key() == winner_pubkey,
                EscrowError::InvalidWinner
            );
        }

        let creator = self.game_escrow.creator;
        let bump = self.game_escrow.bump;
        let game_id = self.game_escrow.game_id;
        let escrow_info = self.game_escrow.to_account_info();

        let rent = Rent::get()?;
        let min_balance = rent.minimum_balance(escrow_info.data_len());
        let balance = escrow_info.lamports();
        require!(balance > min_balance, EscrowError::InsufficientBalance);
        let payout_total = balance - min_balance;

        let seeds = &[
            b"game_escrow",
            creator.as_ref(),
            game_id.as_ref(),
            &[bump],
        ];
        let signer_seeds = &[&seeds[..]];

        self.game_escrow.resolved = true;
        self.game_escrow.winner = winner;

        if let Some(_winner_pubkey) = winner {
            let cpi_ctx = CpiContext::new_with_signer(
                self.system_program.to_account_info(),
                Transfer {
                    from: escrow_info,
                    to: self.winner_destination.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx, payout_total)?;
        } else {
            let half = payout_total / 2;
            let other_half = payout_total - half;

            let cpi_ctx_creator = CpiContext::new_with_signer(
                self.system_program.to_account_info(),
                Transfer {
                    from: escrow_info.clone(),
                    to: self.creator.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx_creator, half)?;

            let cpi_ctx_joiner = CpiContext::new_with_signer(
                self.system_program.to_account_info(),
                Transfer {
                    from: escrow_info,
                    to: self.joiner.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx_joiner, other_half)?;
        }

        Ok(())
    }
}
