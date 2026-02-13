# seeker-rps-api

Backend API for Seeker RPS. Persistence: **MongoDB**.

## Project structure

```
api/src/
├── main.rs      # Entry point: logger, config, DB connection, router assembly
├── lib.rs       # Module tree (config, error, games, health)
├── config.rs    # Config from env (MONGODB_URI, MONGODB_DB_NAME)
├── error.rs     # ApiError type + IntoResponse for consistent error JSON
├── health.rs    # Root and /health routes (no state)
└── games.rs     # Games domain: types, handlers, routes + AppState
```

**Good practices used:**

- **Config**: All env vars in `config.rs`; `main` stays thin.
- **Errors**: Handlers return `Result<T, ApiError>`; `ApiError` implements `IntoResponse` so errors become `{ "error": "message" }` with the right status code.
- **Modules by domain**: One module per area (health, games). When a module grows, split into `games/mod.rs`, `games/handlers.rs`, `games/models.rs`.
- **Logging**: `log` + `env_logger`; level via `RUST_LOG`.

**Optional next steps:** request logging middleware (e.g. `tower_http::Trace`), validation layer for bodies (e.g. `validator`), dedicated `app.rs` that builds the `Router` so `main` only calls `run(config)`.

## Collections

- **users**: `{ pubkey }` — one document per wallet
- **games**: `{ _id, pin, creator_pubkey, joiner_pubkey?, status, created_at }` — exactly 2 players per game (creator + joiner; joiner_pubkey set when second player joins)

## Run locally

1. Set `MONGODB_URI` and `MONGODB_DB_NAME`.
2. Start the API:

```bash
export MONGODB_URI="mongodb://localhost:27017"
export MONGODB_DB_NAME="rps"
# export RUST_LOG=debug   # optional: info (default), debug, trace
cargo run
```

Server listens on `0.0.0.0:3000`. Logs are controlled by `RUST_LOG` (default: `info`).

## Deploy

API base URL for the develop environment: **https://api.develop.rps.kevred.com**

Ensure `MONGODB_URI` and `MONGODB_DB_NAME` are set in the deployment environment.
