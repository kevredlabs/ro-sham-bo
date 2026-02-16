//! RPS game escrow program (Anchor).
//!
//! - **create_game(game_id, amount)**: Creator initializes escrow for one game (game_id = MongoDB _id as 16 bytes) and deposits `amount` SOL.
//! - **join_game**: Second player deposits the same amount into the same PDA.
//! - **resolve(winner)**: Authority resolves the game: all SOL to `winner`, or 50/50 on tie.
//!
//! PDA seeds: ["game_escrow", creator, game_id] so one creator can have multiple games.

use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};

declare_id!("F4d4VwBaQrqf5hUZs74XoiVCAo76BpeRSqABxMMzG7kN");

#[program]
pub mod rps_escrow {
    use super::*;

    /// Creator creates a game escrow and deposits `amount` lamports into the game PDA.
    /// `game_id` must match the MongoDB game _id as 16 bytes (UUID without hyphens).
    pub fn create_game(ctx: Context<CreateGame>, game_id: [u8; 16], amount: u64) -> Result<()> {
        require!(amount > 0, EscrowError::InvalidAmount);

        let state = &mut ctx.accounts.game_escrow;
        state.creator = ctx.accounts.creator.key();
        state.game_id = game_id;
        state.joiner = None;
        state.amount_per_player = amount;
        state.bump = ctx.bumps.game_escrow;
        state.resolved = false;
        state.winner = None;

        let cpi_ctx = CpiContext::new(
            ctx.accounts.system_program.to_account_info(),
            Transfer {
                from: ctx.accounts.creator.to_account_info(),
                to: ctx.accounts.game_escrow.to_account_info(),
            },
        );
        transfer(cpi_ctx, amount)?;

        Ok(())
    }

    /// Joiner deposits the same amount as the creator into the escrow.
    pub fn join_game(ctx: Context<JoinGame>) -> Result<()> {
        let state = &ctx.accounts.game_escrow;
        require!(!state.resolved, EscrowError::AlreadyResolved);
        require!(state.joiner.is_none(), EscrowError::JoinerAlreadySet);

        let amount = state.amount_per_player;

        let cpi_ctx = CpiContext::new(
            ctx.accounts.system_program.to_account_info(),
            Transfer {
                from: ctx.accounts.joiner.to_account_info(),
                to: ctx.accounts.game_escrow.to_account_info(),
            },
        );
        transfer(cpi_ctx, amount)?;

        let state = &mut ctx.accounts.game_escrow;
        state.joiner = Some(ctx.accounts.joiner.key());

        Ok(())
    }

    /// Authority resolves the game and pays out SOL.
    /// - If `winner` is Some(pubkey): entire escrow balance (minus rent) goes to that pubkey.
    /// - If `winner` is None (tie): half to creator, half to joiner.
    pub fn resolve(ctx: Context<Resolve>, winner: Option<Pubkey>) -> Result<()> {
        {
            let state = &ctx.accounts.game_escrow;
            require!(!state.resolved, EscrowError::AlreadyResolved);
            require!(state.joiner.is_some(), EscrowError::NoJoiner);
            require!(
                winner.is_none()
                    || winner == Some(state.creator)
                    || state.joiner == winner,
                EscrowError::InvalidWinner
            );
        }

        let creator = ctx.accounts.game_escrow.creator;
        let bump = ctx.accounts.game_escrow.bump;
        let escrow_info = ctx.accounts.game_escrow.to_account_info();
        let rent = Rent::get()?;
        let min_balance = rent.minimum_balance(escrow_info.data_len());
        let balance = escrow_info.lamports();
        require!(balance > min_balance, EscrowError::InsufficientBalance);
        let payout_total = balance - min_balance;

        let game_id = ctx.accounts.game_escrow.game_id;
        let seeds = &[
            b"game_escrow",
            creator.as_ref(),
            game_id.as_ref(),
            &[bump],
        ];
        let signer_seeds = &[&seeds[..]];

        ctx.accounts.game_escrow.resolved = true;
        ctx.accounts.game_escrow.winner = winner;

        if let Some(_winner_pubkey) = winner {
            let cpi_ctx = CpiContext::new_with_signer(
                ctx.accounts.system_program.to_account_info(),
                Transfer {
                    from: escrow_info,
                    to: ctx.accounts.winner_destination.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx, payout_total)?;
        } else {
            let half = payout_total / 2;
            let other_half = payout_total - half;

            let cpi_ctx_creator = CpiContext::new_with_signer(
                ctx.accounts.system_program.to_account_info(),
                Transfer {
                    from: escrow_info.clone(),
                    to: ctx.accounts.creator.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx_creator, half)?;

            let cpi_ctx_joiner = CpiContext::new_with_signer(
                ctx.accounts.system_program.to_account_info(),
                Transfer {
                    from: escrow_info,
                    to: ctx.accounts.joiner.to_account_info(),
                },
                signer_seeds,
            );
            transfer(cpi_ctx_joiner, other_half)?;
        }

        Ok(())
    }
}

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

#[account]
#[derive(InitSpace)]
pub struct GameEscrow {
    pub creator: Pubkey,
    /// MongoDB game _id as 16 bytes (UUID without hyphens). Matches DB field _id.
    pub game_id: [u8; 16],
    pub joiner: Option<Pubkey>,
    pub amount_per_player: u64,
    pub bump: u8,
    pub resolved: bool,
    pub winner: Option<Pubkey>,
}

#[error_code]
pub enum EscrowError {
    #[msg("Amount must be greater than zero")]
    InvalidAmount,
    #[msg("Game already resolved")]
    AlreadyResolved,
    #[msg("Joiner already set")]
    JoinerAlreadySet,
    #[msg("No joiner has deposited yet")]
    NoJoiner,
    #[msg("Insufficient balance in escrow")]
    InsufficientBalance,
    #[msg("Winner must be creator or joiner")]
    InvalidWinner,
}
