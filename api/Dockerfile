# Build stage
FROM rust:1.93-bookworm AS builder

WORKDIR /app

# Copy manifests and source
COPY Cargo.toml Cargo.lock* ./
COPY src ./src

# Build release binary
RUN cargo build --release

# Runtime stage
FROM debian:bookworm-slim

RUN apt-get update -y \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/seeker-rps-api /usr/local/bin/

EXPOSE 3000

ENTRYPOINT ["/usr/local/bin/seeker-rps-api"]
