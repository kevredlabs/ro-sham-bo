//! Games and users with MongoDB persistence.
//!
//! **Collections:**
//! - **users**: { pubkey } — one doc per wallet
//! - **games**: { _id, pin, creator_pubkey, joiner_pubkey, status, created_at } — exactly 2 players per game

use axum::{
    extract::State,
    routing::post,
    Json, Router,
};
use mongodb::{bson::doc, Database};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::error::ApiError;

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

/// Shared state for routes: MongoDB database.
#[derive(Clone)]
pub struct AppState {
    pub db: Database,
}

/// Generates a 4-digit numeric PIN (0000-9999).
fn generate_pin() -> String {
    use rand::Rng;
    format!("{:04}", rand::thread_rng().gen_range(0..10000))
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
        .with_state(state)
}
