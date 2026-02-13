# seeker-rps-api

Backend API for Seeker RPS. Persistence: **MongoDB**.

## Collections

- **users**: `{ pubkey }` — one document per wallet
- **games**: `{ _id, pin, creator_pubkey, joiner_pubkey?, status, created_at }` — exactly 2 players per game (creator + joiner; joiner_pubkey set when second player joins)

## Run locally

1. Set `MONGODB_URI` (e.g. `mongodb://localhost:27017` or Atlas connection string).
2. Start the API:

```bash
export MONGODB_URI="mongodb://localhost:27017"
cargo run
```

Server listens on `0.0.0.0:3000`.

## Deploy

API base URL for the develop environment: **https://api.develop.rps.kevred.com**

Ensure `MONGODB_URI` is set in the deployment environment.
