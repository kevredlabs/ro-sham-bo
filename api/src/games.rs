//! Games and users with MongoDB persistence.
//!
//! **Collections:**
//! - **users**: { pubkey } — one doc per wallet
//! - **games**: { _id, pin, creator_pubkey, joiner_pubkey, status, created_at } — exactly 2 players per game

use axum::{
    extract::{Path, State},
    routing::{get, post},
    Json, Router,
};

use crate::auth::AuthUser;
use mongodb::{
    bson::doc,
    options::IndexOptions,
    Collection,
    Database,
    IndexModel,
};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::error::ApiError;
use crate::solana::{self, SolanaAppClient};

/// Path parameter for game ID.
#[derive(Deserialize)]
pub struct GameIdPath {
    pub game_id: String,
}

/// Request body for joining an existing game by PIN. Joiner identity from SIWS auth.
#[derive(Deserialize)]
pub struct JoinGameRequest {
    pub pin: String,
}

/// Response when joining a game successfully.
#[derive(Serialize)]
pub struct JoinGameResponse {
    pub game_id: String,
}

/// Request body for cancelling a game (optional). Creator identity from SIWS auth.
#[derive(Deserialize, Default)]
pub struct CancelGameRequest {}

/// Request body for submitting a RPS choice. Player identity from SIWS auth.
#[derive(Deserialize)]
pub struct SubmitChoiceRequest {
    pub choice: String,
}

#[derive(Clone, Serialize, Deserialize)]
pub struct Game {
    #[serde(rename = "_id")]
    pub id: String,
    pub pin: String,
    pub creator_pubkey: String,
    /// Set when the second player joins (by PIN).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub joiner_pubkey: Option<String>,
    pub status: GameStatus,
    pub created_at: String,
    /// Creator's choice: "rock", "paper", or "scissors".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub creator_choice: Option<String>,
    /// Joiner's choice.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub joiner_choice: Option<String>,
    /// Winner's pubkey when both have chosen; null if draw.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub winner_pubkey: Option<String>,
    /// True when the last round was a draw and choices were cleared for the next round.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub round_cleared_for_draw: Option<bool>,
    /// Stake amount per player in lamports.
    #[serde(default)]
    pub amount_per_player: i64,
    /// Game escrow PDA (base58).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub game_escrow_pubkey: Option<String>,
    /// Vault PDA (base58).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vault_pubkey: Option<String>,
    /// On-chain resolve transaction signature (set on success).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolve_tx: Option<String>,
    /// Error message when on-chain resolve failed (status = resolve_failed).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolve_error: Option<String>,
}

#[derive(Clone, Copy, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum GameStatus {
    #[default]
    Waiting,
    Active,
    /// Winner computed, on-chain resolve in progress.
    Resolving,
    Finished,
    /// On-chain resolve failed; needs manual retry.
    ResolveFailed,
    Cancelled,
}

#[derive(Clone, Serialize, Deserialize)]
pub struct User {
    pub pubkey: String,
}

/// Request body for creating a game. Creator identity from SIWS auth.
#[derive(Deserialize)]
pub struct CreateGameRequest {
    pub game_id: Option<String>,
    #[serde(default)]
    pub amount_per_player: i64,
}

#[derive(Serialize)]
pub struct CreateGameResponse {
    pub game_id: String,
    pub pin: String,
}

/// Shared state for routes: MongoDB database and optional Solana client for resolve.
#[derive(Clone)]
pub struct AppState {
    pub db: Database,
    pub solana: Option<SolanaAppClient>,
}

/// Max attempts when reserving a PIN to avoid collision with an existing waiting game.
const PIN_RESERVE_MAX_ATTEMPTS: u32 = 25;

/// Generates a 4-digit numeric PIN (0000-9999).
fn generate_pin() -> String {
    use rand::Rng;
    format!("{:04}", rand::thread_rng().gen_range(0..10000))
}

/// Ensures the partial unique index on `pin` for waiting games exists.
/// Uniqueness applies only to documents with status "waiting" and no joiner, so the same PIN
/// can be reused after a game is finished or cancelled.
pub async fn ensure_games_pin_index(db: &Database) -> Result<(), mongodb::error::Error> {
    let options = IndexOptions::builder()
        .unique(true)
        .partial_filter_expression(doc! {
            "status": "waiting",
            "joiner_pubkey": null,
        })
        .build();
    let model = IndexModel::builder()
        .keys(doc! { "pin": 1 })
        .options(options)
        .build();
    db.collection::<mongodb::bson::Document>("games")
        .create_index(model, None)
        .await?;
    log::info!("Games PIN index (unique for waiting games) ensured");
    Ok(())
}

/// Returns a PIN that is not used by any current waiting game (retries on collision).
async fn reserve_pin(db: &Database) -> Result<String, ApiError> {
    let games = db.collection::<Game>("games");
    for _ in 0..PIN_RESERVE_MAX_ATTEMPTS {
        let pin = generate_pin();
        let existing = games
            .find_one(
                doc! {
                    "pin": &pin,
                    "status": "waiting",
                    "joiner_pubkey": null,
                },
                None,
            )
            .await
            .map_err(|e| {
                log::error!("Failed to check PIN availability: {}", e);
                ApiError::internal(e.to_string())
            })?;
        if existing.is_none() {
            return Ok(pin);
        }
        log::debug!("PIN collision, retrying with new PIN");
    }
    log::error!("Failed to reserve a unique PIN after {} attempts", PIN_RESERVE_MAX_ATTEMPTS);
    Err(ApiError::internal(
        "Could not generate a unique PIN; please try again",
    ))
}

/// Minimum bet per player in lamports (0.001 SOL). Must match program MIN_BET_LAMPORTS.
const MIN_BET_LAMPORTS: i64 = 1_000_000;

const VALID_CHOICES: [&str; 3] = ["rock", "paper", "scissors"];

/// Shortens a pubkey for log display (first 6 chars + "..." + last 6 chars).
fn short_pk(pk: &str) -> String {
    let chars: Vec<char> = pk.chars().collect();
    let n = chars.len();
    if n <= 12 {
        pk.to_string()
    } else {
        format!(
            "{}...{}",
            chars[..6].iter().collect::<String>(),
            chars[n - 6..].iter().collect::<String>()
        )
    }
}

/// Returns (winner_choice, loser_choice, winner_pubkey) or None if draw.
fn compute_winner(
    creator_choice: &str,
    joiner_choice: &str,
    creator_pubkey: &str,
    joiner_pubkey: &str,
) -> Option<(String, String, String)> {
    let c = creator_choice.to_lowercase();
    let j = joiner_choice.to_lowercase();
    if c == j {
        return None;
    }
    let creator_wins = matches!(
        (c.as_str(), j.as_str()),
        ("rock", "scissors") | ("paper", "rock") | ("scissors", "paper")
    );
    if creator_wins {
        Some((c, j, creator_pubkey.to_string()))
    } else {
        Some((j, c, joiner_pubkey.to_string()))
    }
}

async fn create_game(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(body): Json<CreateGameRequest>,
) -> Result<Json<CreateGameResponse>, ApiError> {
    let creator_pubkey = auth.pubkey.trim();
    if body.amount_per_player < MIN_BET_LAMPORTS {
        log::warn!("Create game rejected: amount {} below minimum {}", body.amount_per_player, MIN_BET_LAMPORTS);
        return Err(ApiError::bad_request("amount_per_player must be at least 0.001 SOL (1_000_000 lamports)"));
    }

    let game_id = body.game_id.clone()
        .filter(|id| Uuid::parse_str(id).is_ok())
        .unwrap_or_else(|| Uuid::new_v4().to_string());
    let pin = reserve_pin(&state.db).await?;
    let created_at = chrono::Utc::now().format("%Y-%m-%d %H:%M:%S UTC").to_string();

    let (game_escrow_pubkey, vault_pubkey) = {
        let program_id = state.solana.as_ref()
            .map(|s| s.program_id)
            .unwrap_or_else(solana::program_id);
        let creator_pk: solana_sdk::pubkey::Pubkey = creator_pubkey.parse()
            .map_err(|_| ApiError::bad_request("invalid creator_pubkey (not a valid base58 pubkey)"))?;
        let uuid = Uuid::parse_str(&game_id).unwrap();
        let game_id_bytes: [u8; 16] = *uuid.as_bytes();
        let escrow = solana::game_escrow_pda(&program_id, &creator_pk, &game_id_bytes);
        let vault = solana::vault_pda(&program_id, &escrow);
        (escrow.to_string(), vault.to_string())
    };

    let game = Game {
        id: game_id.clone(),
        pin: pin.clone(),
        creator_pubkey: creator_pubkey.to_string(),
        joiner_pubkey: None,
        status: GameStatus::Waiting,
        created_at,
        creator_choice: None,
        joiner_choice: None,
        winner_pubkey: None,
        round_cleared_for_draw: None,
        amount_per_player: body.amount_per_player,
        game_escrow_pubkey: Some(game_escrow_pubkey),
        vault_pubkey: Some(vault_pubkey),
        resolve_tx: None,
        resolve_error: None,
    };

    let users = state.db.collection::<User>("users");
    let games = state.db.collection::<Game>("games");

    // Ensure creator exists in users (upsert by pubkey)
    users
        .update_one(
            doc! { "pubkey": creator_pubkey },
            doc! { "$setOnInsert": { "pubkey": creator_pubkey } },
            mongodb::options::UpdateOptions::builder()
                .upsert(true)
                .build(),
        )
        .await
        .map_err(|e| {
            log::error!("Failed to upsert user: {}", e);
            ApiError::internal(e.to_string())
        })?;

    games
        .insert_one(&game, None)
        .await
        .map_err(|e| {
            log::error!("Failed to insert game: {}", e);
            ApiError::internal(e.to_string())
        })?;

    log::info!("Game created game_id={} creator_pubkey={}", game_id, creator_pubkey);
    Ok(Json(CreateGameResponse { game_id, pin }))
}

async fn get_game(
    State(state): State<AppState>,
    Path(path): Path<GameIdPath>,
) -> Result<Json<Game>, ApiError> {
    let games = state.db.collection::<Game>("games");
    let filter = doc! { "_id": &path.game_id };
    let game = games
        .find_one(filter, None)
        .await
        .map_err(|e| {
            log::error!("Failed to find game: {}", e);
            ApiError::internal(e.to_string())
        })?;
    match game {
        Some(g) => Ok(Json(g)),
        None => Err(ApiError::not_found("Game not found")),
    }
}

async fn submit_choice(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(path): Path<GameIdPath>,
    Json(body): Json<SubmitChoiceRequest>,
) -> Result<Json<Game>, ApiError> {
    let pubkey = auth.pubkey.trim();
    let choice = body.choice.trim().to_lowercase();
    if !VALID_CHOICES.contains(&choice.as_str()) {
        return Err(ApiError::bad_request("choice must be rock, paper, or scissors"));
    }

    let games = state.db.collection::<Game>("games");
    let filter = doc! { "_id": &path.game_id };
    let game = games
        .find_one(filter.clone(), None)
        .await
        .map_err(|e| {
            log::error!("Failed to find game: {}", e);
            ApiError::internal(e.to_string())
        })?;

    let game = match game {
        Some(g) => g,
        None => return Err(ApiError::not_found("Game not found")),
    };

    let joiner_pubkey = match &game.joiner_pubkey {
        Some(j) => j.as_str(),
        None => return Err(ApiError::bad_request("Game has no second player yet")),
    };

    let (_choice_field, is_creator) = if pubkey == game.creator_pubkey {
        ("creator_choice", true)
    } else if pubkey == joiner_pubkey {
        ("joiner_choice", false)
    } else {
        return Err(ApiError::bad_request("You are not a player in this game"));
    };

    // If previous round was a draw, clear both choices and the flag before applying the new choice.
    let update_doc = if game.round_cleared_for_draw == Some(true) {
        if is_creator {
            doc! {
                "$set": {
                    "creator_choice": choice.as_str(),
                    "joiner_choice": null,
                    "round_cleared_for_draw": false
                }
            }
        } else {
            doc! {
                "$set": {
                    "joiner_choice": choice.as_str(),
                    "creator_choice": null,
                    "round_cleared_for_draw": false
                }
            }
        }
    } else {
        if is_creator {
            doc! { "$set": { "creator_choice": choice.as_str() } }
        } else {
            doc! { "$set": { "joiner_choice": choice.as_str() } }
        }
    };

    games
        .update_one(filter.clone(), update_doc, None)
        .await
        .map_err(|e| {
            log::error!("Failed to update choice: {}", e);
            ApiError::internal(e.to_string())
        })?;

    // Reload game and check if both have chosen; if so, compute winner and set status.
    let updated = games
        .find_one(filter, None)
        .await
        .map_err(|e| {
            log::error!("Failed to find game after update: {}", e);
            ApiError::internal(e.to_string())
        })?;

    let mut game = match updated {
        Some(g) => g,
        None => return Err(ApiError::not_found("Game not found")),
    };

    if let (Some(cc), Some(jc)) = (&game.creator_choice, &game.joiner_choice) {
        let creator_short = short_pk(&game.creator_pubkey);
        let joiner_short = short_pk(game.joiner_pubkey.as_deref().unwrap_or(""));
        let winner_pubkey = compute_winner(
            cc,
            jc,
            &game.creator_pubkey,
            game.joiner_pubkey.as_deref().unwrap_or(""),
        )
        .map(|(_, _, wp)| wp);

        if let Some(ref winner) = winner_pubkey {
            let winner_short = short_pk(winner);
            log::info!(
                "Round complete game_id={} creator={} choice={} joiner={} choice={} winner={}",
                path.game_id,
                creator_short,
                cc,
                joiner_short,
                jc,
                winner_short
            );

            // Step 1: mark as resolving (winner known, waiting for on-chain confirmation)
            games
                .update_one(
                    doc! { "_id": &path.game_id },
                    doc! {
                        "$set": {
                            "winner_pubkey": winner.clone(),
                            "status": "resolving",
                            "resolve_error": null,
                            "resolve_tx": null
                        }
                    },
                    None,
                )
                .await
                .map_err(|e| {
                    log::error!("Failed to set winner/resolving: {}", e);
                    ApiError::internal(e.to_string())
                })?;
            game.winner_pubkey = Some(winner.clone());
            game.status = GameStatus::Resolving;

            // Step 2: attempt on-chain resolve
            let resolve_result = try_resolve_on_chain(
                &state, &games, &path.game_id, &game.creator_pubkey, winner,
            ).await;
            match resolve_result {
                Ok(sig) => {
                    game.status = GameStatus::Finished;
                    game.resolve_tx = Some(sig);
                }
                Err(err_msg) => {
                    game.status = GameStatus::ResolveFailed;
                    game.resolve_error = Some(err_msg);
                }
            }
        } else {
            // Draw: clear choices and set round_cleared_for_draw so both clients start next round.
            log::info!(
                "Round complete game_id={} creator={} choice={} joiner={} choice={} draw",
                path.game_id,
                creator_short,
                cc,
                joiner_short,
                jc
            );
            games
                .update_one(
                    doc! { "_id": &path.game_id },
                    doc! {
                        "$set": {
                            "creator_choice": null,
                            "joiner_choice": null,
                            "round_cleared_for_draw": true
                        }
                    },
                    None,
                )
                .await
                .map_err(|e| {
                    log::error!("Failed to clear choices for draw: {}", e);
                    ApiError::internal(e.to_string())
                })?;
            game.creator_choice = None;
            game.joiner_choice = None;
            game.winner_pubkey = None;
            game.round_cleared_for_draw = Some(true);
        }
    }

    Ok(Json(game))
}

async fn cancel_game(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(path): Path<GameIdPath>,
    Json(_body): Json<CancelGameRequest>,
) -> Result<Json<Game>, ApiError> {
    let creator_pubkey = auth.pubkey.trim();

    let games = state.db.collection::<Game>("games");
    let filter = doc! {
        "_id": &path.game_id,
        "creator_pubkey": creator_pubkey,
        "status": "waiting",
        "joiner_pubkey": null,
    };
    let update = doc! {
        "$set": { "status": "cancelled" }
    };
    let opts = mongodb::options::FindOneAndUpdateOptions::builder()
        .return_document(mongodb::options::ReturnDocument::After)
        .build();
    let updated = games
        .find_one_and_update(filter, update, opts)
        .await
        .map_err(|e| {
            log::error!("Failed to cancel game: {}", e);
            ApiError::internal(e.to_string())
        })?;

    match updated {
        Some(game) => {
            log::info!("Game cancelled game_id={} creator_pubkey={}", game.id, creator_pubkey);
            Ok(Json(game))
        }
        None => Err(ApiError::not_found(
            "Game not found, already joined, or not owned by this creator",
        )),
    }
}

/// Look up a waiting game by PIN without modifying it (read-only).
async fn lookup_game_by_pin(
    State(state): State<AppState>,
    Path(pin): Path<String>,
) -> Result<Json<Game>, ApiError> {
    let pin = pin.trim();
    if pin.len() != 4 || !pin.chars().all(|c| c.is_ascii_digit()) {
        return Err(ApiError::bad_request("pin must be 4 digits"));
    }
    let games = state.db.collection::<Game>("games");
    let filter = doc! {
        "pin": pin,
        "joiner_pubkey": null,
        "status": "waiting"
    };
    let game = games
        .find_one(filter, None)
        .await
        .map_err(|e| {
            log::error!("Failed to lookup game by pin: {}", e);
            ApiError::internal(e.to_string())
        })?;
    match game {
        Some(g) => Ok(Json(g)),
        None => Err(ApiError::not_found("No waiting game found for this PIN")),
    }
}

async fn join_game(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(body): Json<JoinGameRequest>,
) -> Result<Json<JoinGameResponse>, ApiError> {
    let pin = body.pin.trim();
    let joiner_pubkey = auth.pubkey.trim();
    if pin.len() != 4 || !pin.chars().all(|c| c.is_ascii_digit()) {
        log::warn!("Join game rejected: invalid pin (must be 4 digits)");
        return Err(ApiError::bad_request("pin must be 4 digits"));
    }

    let games = state.db.collection::<Game>("games");
    let users = state.db.collection::<User>("users");

    // Ensure joiner exists in users
    users
        .update_one(
            doc! { "pubkey": joiner_pubkey },
            doc! { "$setOnInsert": { "pubkey": joiner_pubkey } },
            mongodb::options::UpdateOptions::builder()
                .upsert(true)
                .build(),
        )
        .await
        .map_err(|e| {
            log::error!("Failed to upsert joiner user: {}", e);
            ApiError::internal(e.to_string())
        })?;

    let filter = doc! {
        "pin": pin,
        "joiner_pubkey": null,
        "status": "waiting"
    };
    let update = doc! {
        "$set": {
            "joiner_pubkey": joiner_pubkey,
            "status": "active"
        }
    };
    let opts = mongodb::options::FindOneAndUpdateOptions::builder()
        .return_document(mongodb::options::ReturnDocument::After)
        .build();
    let updated = games
        .find_one_and_update(filter, update, opts)
        .await
        .map_err(|e| {
            log::error!("Failed to join game: {}", e);
            ApiError::internal(e.to_string())
        })?;

    match updated {
        Some(game) => {
            log::info!("Game joined game_id={} joiner_pubkey={}", game.id, joiner_pubkey);
            Ok(Json(JoinGameResponse { game_id: game.id }))
        }
        None => {
            log::warn!("Join game failed: no waiting game for pin");
            Err(ApiError::not_found("No Game Available for this PIN"))
        }
    }
}

/// Attempts on-chain resolve. On success updates DB to `finished` + stores tx sig.
/// On failure updates DB to `resolve_failed` + stores error.
/// Returns Ok(signature) or Err(error_message).
async fn try_resolve_on_chain(
    state: &AppState,
    games: &Collection<Game>,
    game_id: &str,
    creator_pubkey: &str,
    winner_pubkey: &str,
) -> Result<String, String> {
    let solana = match &state.solana {
        Some(s) if s.can_resolve() => s,
        _ => {
            let msg = "Solana resolve not configured".to_string();
            log::error!("try_resolve_on_chain: {}", msg);
            games
                .update_one(
                    doc! { "_id": game_id },
                    doc! { "$set": { "status": "resolve_failed", "resolve_error": &msg } },
                    None,
                )
                .await
                .ok();
            return Err(msg);
        }
    };

    let game_id_bytes = match uuid::Uuid::parse_str(game_id) {
        Ok(u) => *u.as_bytes(),
        Err(e) => {
            let msg = format!("invalid game_id UUID: {}", e);
            log::error!("try_resolve_on_chain: {}", msg);
            games
                .update_one(
                    doc! { "_id": game_id },
                    doc! { "$set": { "status": "resolve_failed", "resolve_error": &msg } },
                    None,
                )
                .await
                .ok();
            return Err(msg);
        }
    };

    match solana.resolve(game_id_bytes, creator_pubkey, winner_pubkey) {
        Ok(res) => {
            log::info!(
                "Game resolved on-chain game_id={} winner={} sig={}",
                game_id, winner_pubkey, res.signature
            );
            games
                .update_one(
                    doc! { "_id": game_id },
                    doc! { "$set": {
                        "status": "finished",
                        "resolve_tx": &res.signature,
                        "resolve_error": null
                    }},
                    None,
                )
                .await
                .map_err(|e| {
                    log::error!("Failed to update game to finished: {}", e);
                    e.to_string()
                })?;
            Ok(res.signature)
        }
        Err(e) => {
            log::error!(
                "On-chain resolve failed game_id={} winner={}: {}",
                game_id, winner_pubkey, e
            );
            games
                .update_one(
                    doc! { "_id": game_id },
                    doc! { "$set": { "status": "resolve_failed", "resolve_error": &e } },
                    None,
                )
                .await
                .ok();
            Err(e)
        }
    }
}

pub fn games_routes(state: AppState) -> Router {
    Router::new()
        .route("/games/create", post(create_game))
        .route("/games/join", post(join_game))
        .route("/games/lookup/:pin", get(lookup_game_by_pin))
        .route("/games/:game_id", get(get_game))
        .route("/games/:game_id/choice", post(submit_choice))
        .route("/games/:game_id/cancel", post(cancel_game))
        .with_state(state)
}
