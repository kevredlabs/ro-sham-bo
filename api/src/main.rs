use axum::{routing::get, Json, Router};
use seeker_rps_api::games::{games_routes, AppState};
use serde::Serialize;
use std::net::SocketAddr;
use tower_http::cors::{Any, CorsLayer};

const DB_NAME: &str = "seeker_rps";

#[derive(Serialize)]
struct Health {
    status: &'static str,
    service: &'static str,
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

#[tokio::main]
async fn main() {
    let mongodb_uri = std::env::var("MONGODB_URI").expect("MONGODB_URI must be set");
    let client = mongodb::Client::with_uri_str(&mongodb_uri)
        .await
        .expect("MongoDB connection failed");
    let db = client.database(DB_NAME);
    let state = AppState { db };

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = Router::new()
        .route("/", get(root))
        .route("/health", get(health))
        .merge(games_routes(state))
        .layer(cors);

    let addr = SocketAddr::from(([0, 0, 0, 0], 3000));
    let listener = tokio::net::TcpListener::bind(addr).await.expect("bind");
    axum::serve(listener, app).await.expect("serve");
}
