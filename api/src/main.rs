use seeker_rps_api::config::Config;
use seeker_rps_api::games::{games_routes, AppState};
use seeker_rps_api::health;
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
    let state = AppState { db };
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
