//! Games and users with MongoDB persistence.
//!
//! **Collections:**
//! - **users**: { pubkey } — one doc per wallet
//! - **games**: { _id, pin, creator_pubkey, joiner_pubkey, status, created_at } — exactly 2 players per game

use axum::{
    extract::State,
    http::StatusCode,
    routing::post,
    Json, Router,
};
use mongodb::{bson::doc, Database};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

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
) -> Result<Json<CreateGameResponse>, (StatusCode, String)> {
    let creator_pubkey = body.creator_pubkey.trim();
    if creator_pubkey.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "creator_pubkey is required".to_string(),
        ));
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
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    games
        .insert_one(&game, None)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    Ok(Json(CreateGameResponse { game_id, pin }))
}

pub fn games_routes(state: AppState) -> Router {
    Router::new()
        .route("/games", post(create_game))
        .with_state(state)
}
