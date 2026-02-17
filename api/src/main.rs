use seeker_rps_api::config::Config;
use seeker_rps_api::games::{games_routes, AppState};
use seeker_rps_api::health;
use seeker_rps_api::solana::SolanaAppClient;
use std::net::SocketAddr;
use tower_http::cors::{Any, CorsLayer};

#[tokio::main]
async fn main() {
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info")).init();

    let config = Config::from_env();
    log::info!("Connecting to MongoDB database {}", config.db_name);
    let client = mongodb::Client::with_uri_str(&config.mongodb_uri)
        .await
        .expect("MongoDB connection failed");
    let db = client.database(&config.db_name);
    let solana = Some(SolanaAppClient::from_config(&config));
    if solana.as_ref().map(|s| s.can_resolve()).unwrap_or(false) {
        log::info!("Solana devnet: resolve enabled (program {})", config.rps_escrow_program_id);
    } else {
        log::info!("Solana devnet: resolve disabled (set RESOLVE_AUTHORITY_KEYPAIR_PATH to enable)");
    }
    let state = AppState { db, solana };
    log::info!("MongoDB connected");

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = axum::Router::new()
        .merge(health::routes())
        .merge(games_routes(state))
        .layer(cors);

    let addr = SocketAddr::from(([0, 0, 0, 0], 3000));
    log::info!("Listening on {}", addr);
    let listener = tokio::net::TcpListener::bind(addr).await.expect("bind");
    axum::serve(listener, app).await.expect("serve");
}
