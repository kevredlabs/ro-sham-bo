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
use mongodb::{bson::doc, Database};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::error::ApiError;
use crate::solana::SolanaAppClient;

/// Path parameter for game ID.
#[derive(Deserialize)]
pub struct GameIdPath {
    pub game_id: String,
}

/// Request body for joining an existing game by PIN.
#[derive(Deserialize)]
pub struct JoinGameRequest {
    pub pin: String,
    pub joiner_pubkey: String,
}

/// Response when joining a game successfully.
#[derive(Serialize)]
pub struct JoinGameResponse {
    pub game_id: String,
}

/// Request body for submitting a RPS choice.
#[derive(Deserialize)]
pub struct SubmitChoiceRequest {
    pub pubkey: String,
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
    pub created_at: i64,
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
}

#[derive(Clone, Copy, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum GameStatus {
    #[default]
    Waiting,
    Active,
    Finished,
}

#[derive(Clone, Serialize, Deserialize)]
pub struct User {
    pub pubkey: String,
}

#[derive(Deserialize)]
pub struct CreateGameRequest {
    pub creator_pubkey: String,
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

/// Generates a 4-digit numeric PIN (0000-9999).
fn generate_pin() -> String {
    use rand::Rng;
    format!("{:04}", rand::thread_rng().gen_range(0..10000))
}

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
    Json(body): Json<CreateGameRequest>,
) -> Result<Json<CreateGameResponse>, ApiError> {
    let creator_pubkey = body.creator_pubkey.trim();
    if creator_pubkey.is_empty() {
        log::warn!("Create game rejected: missing creator_pubkey");
        return Err(ApiError::bad_request("creator_pubkey is required"));
    }

    let game_id = Uuid::new_v4().to_string();
    let pin = generate_pin();
    let created_at = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64;

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
    Path(path): Path<GameIdPath>,
    Json(body): Json<SubmitChoiceRequest>,
) -> Result<Json<Game>, ApiError> {
    let pubkey = body.pubkey.trim();
    let choice = body.choice.trim().to_lowercase();
    if pubkey.is_empty() {
        return Err(ApiError::bad_request("pubkey is required"));
    }
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

    let (choice_field, is_creator) = if pubkey == game.creator_pubkey {
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
            // Clear winner: set finished and resolve on-chain once.
            games
                .update_one(
                    doc! { "_id": &path.game_id },
                    doc! {
                        "$set": {
                            "winner_pubkey": winner.clone(),
                            "status": "finished"
                        }
                    },
                    None,
                )
                .await
                .map_err(|e| {
                    log::error!("Failed to set winner: {}", e);
                    ApiError::internal(e.to_string())
                })?;
            game.winner_pubkey = Some(winner.clone());
            game.status = GameStatus::Finished;

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

            if let Some(ref solana) = state.solana {
                if solana.can_resolve() {
                    let game_id_bytes = uuid::Uuid::parse_str(&path.game_id)
                        .ok()
                        .map(|u| *u.as_bytes());
                    if let Some(gid) = game_id_bytes {
                        match solana.resolve(gid, &game.creator_pubkey, winner) {
                            Ok(res) => {
                                log::info!(
                                    "Game resolved on-chain game_id={} winner={} sig={}",
                                    path.game_id,
                                    winner,
                                    res.signature
                                );
                            }
                            Err(e) => {
                                log::error!(
                                    "On-chain resolve failed game_id={} winner={}: {}",
                                    path.game_id,
                                    winner,
                                    e
                                );
                            }
                        }
                    }
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

async fn join_game(
    State(state): State<AppState>,
    Json(body): Json<JoinGameRequest>,
) -> Result<Json<JoinGameResponse>, ApiError> {
    let pin = body.pin.trim();
    let joiner_pubkey = body.joiner_pubkey.trim();
    if pin.len() != 4 || !pin.chars().all(|c| c.is_ascii_digit()) {
        log::warn!("Join game rejected: invalid pin (must be 4 digits)");
        return Err(ApiError::bad_request("pin must be 4 digits"));
    }
    if joiner_pubkey.is_empty() {
        log::warn!("Join game rejected: missing joiner_pubkey");
        return Err(ApiError::bad_request("joiner_pubkey is required"));
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
            Err(ApiError::not_found("No game found for this PIN or game is full"))
        }
    }
}

pub fn games_routes(state: AppState) -> Router {
    Router::new()
        .route("/games/create", post(create_game))
        .route("/games/join", post(join_game))
        .route("/games/:game_id", get(get_game))
        .route("/games/:game_id/choice", post(submit_choice))
        .with_state(state)
}
