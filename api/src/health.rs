//! Health check and root routes (no app state).

use axum::{routing::get, Json, Router};
use serde::Serialize;

#[derive(Serialize)]
pub struct Health {
    pub status: &'static str,
    pub service: &'static str,
}

async fn root() -> &'static str {
    "seeker-rps-api"
}

async fn health() -> Json<Health> {
    Json(Health {
        status: "ok",
        service: "seeker-rps-api",
    })
}

/// Routes for / and /health.
pub fn routes() -> Router {
    Router::new()
        .route("/", get(root))
        .route("/health", get(health))
}
