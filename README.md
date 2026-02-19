# Seeker RPS

A **Rock-Paper-Scissors** game with **Solana escrow**: two players stake SOL per game; the winner receives the full pot after the backend resolves the result on-chain.

## Overview

- **Game flow**: One player creates a game (with a 4-digit PIN), the other joins by PIN. Both submit their choice (rock / paper / scissors). When both have chosen, the backend determines the winner and, if configured, calls the Solana program to pay out the escrowed SOL to the winner.
- **Backend**: REST API (Rust, Axum) stores games and users in **MongoDB**, and optionally triggers on-chain **resolve** using a resolve-authority keypair.
- **On-chain**: Anchor program **rps-escrow** holds SOL in a PDA per game; only a designated authority can call `resolve(winner)` to send all escrowed SOL to the winner and close the account.

## Mobile (Solana Mobile / Seeker)

The game is played on the [**Solana Mobile**](https://solanamobile.com/developers) stack. End-to-end testing is done on the **Seeker**, Solana’s reference mobile device. The mobile client connects to the same API and signs `create_game` / `join_game` transactions on-chain from the device.

## Repository structure

| Path | Description |
|------|-------------|
| `api/` | Rust API (Axum, MongoDB, optional Solana client for resolve) |
| `android/` | Android app (Solana Mobile). Main source: `app/src/main/java/com/solanamobile/ktxclientsample/` (Kotlin — UI, ViewModels, `GameApiUseCase`, `EscrowUseCase`, wallet connect, game flow). |
| `programs/rps-escrow/` | Anchor workspace: Solana program `rps_escrow` |
| `programs/rps-escrow/runbooks/` | [Surfpool](https://surfpool.run) runbooks (e.g. deployment) |
| `programs/rps-escrow/upgrade_authority.json` | Keypair holding the **program upgrade authority**; used to upgrade the deployed program (e.g. via `anchor upgrade` or runbooks). Keep this file secret and out of version control (it is in `.gitignore`). |

## Solana program (rps-escrow)

- **create_game(game_id, amount)** — Creator initializes the escrow PDA and deposits `amount` lamports. `game_id` is 16 bytes (UUID without hyphens, matching the MongoDB game `_id`).
- **join_game** — Second player deposits the same amount into the same PDA.
- **resolve(winner)** — Only the configured **resolve authority** can call this; it sends the full escrow balance to `winner` (must be creator or joiner) and closes the PDA.

PDA seeds: `["game_escrow", creator, game_id]`.

## API

- **POST /games/create** — `{ "creator_pubkey": "..." }` → creates a game in MongoDB, returns `game_id` and 4-digit `pin`.
- **POST /games/join** — `{ "pin": "1234", "joiner_pubkey": "..." }` → joins the game by PIN, returns `game_id`.
- **GET /games/:game_id** — Returns game state (creator, joiner, choices, winner, status).
- **POST /games/:game_id/choice** — `{ "pubkey": "...", "choice": "rock"|"paper"|"scissors" }` → submit choice; when both have chosen, winner is computed and, if Solana is configured, on-chain resolve is triggered.

### API configuration (environment)

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | MongoDB connection string |
| `MONGODB_DB_NAME` | Database name |
| `SOLANA_RPC_URL` | Solana RPC (default: `https://api.devnet.solana.com`) |
| `RPS_ESCROW_PROGRAM_ID` | Deployed rps-escrow program ID |
| `RESOLVE_AUTHORITY_KEYPAIR_PATH` | Path to the resolve authority keypair JSON (required for on-chain resolve) |

Without `RESOLVE_AUTHORITY_KEYPAIR_PATH`, the API still runs and manages games in MongoDB but will not send resolve transactions.

## Android (Solana Mobile)

The mobile frontend is **full Kotlin** (no React Native or hybrid). App lives in `android/`, package `com.solanamobile.ktxclientsample`. It drives the full game flow on device: wallet connection via [Solana Mobile](https://solanamobile.com/developers), create/join game via the REST API, and on-chain **create_game** / **join_game** via `EscrowUseCase`; choices are submitted through the API; the UI (Jetpack Compose) and `GameViewModel` / `GameApiUseCase` handle game state and rounds. Target device for E2E tests: **Seeker**. The mobile app is built and tested with **Android Studio**.

## Building and running

### Program (Anchor)

```bash
cd programs/rps-escrow
anchor build
# Tests
yarn run test
```


To **upgrade** the deployed program, use the keypair in `programs/rps-escrow/upgrade_authority.json` (e.g. `anchor upgrade` with `--provider.cluster` and the wallet set to this keypair, or the deployment runbook). That file must remain secret and is listed in `.gitignore`.

### API

```bash
cd api
cargo build --release
# Set MONGODB_URI, MONGODB_DB_NAME, RPS_ESCROW_PROGRAM_ID, RESOLVE_AUTHORITY_KEYPAIR_PATH (and optionally SOLANA_RPC_URL)
./target/release/seeker-rps-api
```

The API listens on `0.0.0.0:3000`.

### Android (Android Studio)

Open the `android/` project in **Android Studio**, then build and run on an emulator or a **Seeker** device to test the mobile flow.

## Tech stack

- **Solana**: Anchor 0.32, program ID `F4d4VwBaQrqf5hUZs74XoiVCAo76BpeRSqABxMMzG7kN` (localnet/devnet).
- **API**: Rust (Axum, MongoDB driver, Anchor client for resolve).
- **Mobile**: Full Kotlin (Jetpack Compose, Solana Mobile); tested with Android Studio on Seeker.
- **Deployment / tooling**: Surfpool (Surfnet, runbooks).

## License

ISC (see `programs/rps-escrow/package.json`).
