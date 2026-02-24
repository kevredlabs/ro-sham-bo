//! SIWS (Sign-In With Solana) verification and auth extractor.
//!
//! Protects routes by requiring headers X-SIWS-Address, X-SIWS-Message, X-SIWS-Signature.
//! Verifies the signature and optional message expiration, then exposes the pubkey.

use async_trait::async_trait;
use axum::{
    extract::FromRequestParts,
    http::{request::Parts, StatusCode},
    Json,
};
use solana_sdk::{pubkey::Pubkey, signature::Signature};

use crate::error::ApiError;

/// Decodes X-SIWS-Message header (Base64) to the original UTF-8 message. Returns None on error.
fn decode_siws_message_header(base64_value: &str) -> Option<String> {
    if base64_value.is_empty() {
        return Some(String::new());
    }
    use base64::Engine;
    let bytes = base64::engine::general_purpose::STANDARD.decode(base64_value.trim()).ok()?;
    String::from_utf8(bytes).ok()
}

/// Authenticated user: pubkey from verified SIWS proof.
#[derive(Clone, Debug)]
pub struct AuthUser {
    pub pubkey: String,
}

/// Verifies SIWS proof: message was signed by the given address.
/// Optionally parses "Expiration Time:" from message (SIWS format) and rejects if expired.
pub fn verify_siws(
    message: &str,
    signature_base58: &str,
    address: &str,
) -> Result<String, ApiError> {
    let address = address.trim();
    if address.is_empty() {
        return Err(ApiError::unauthorized("missing SIWS address"));
    }
    if message.is_empty() {
        return Err(ApiError::unauthorized("missing SIWS message"));
    }
    if signature_base58.trim().is_empty() {
        return Err(ApiError::unauthorized("missing SIWS signature"));
    }

    let pubkey: Pubkey = address
        .parse()
        .map_err(|_| ApiError::unauthorized("invalid SIWS address (not a valid base58 pubkey)"))?;

    let signature: Signature = signature_base58
        .trim()
        .parse()
        .map_err(|_| ApiError::unauthorized("invalid SIWS signature (not valid base58)"))?;

    let message_bytes = message.as_bytes();
    let ok = signature.verify(pubkey.as_ref(), message_bytes);
    if !ok {
        log::warn!("SIWS verification failed for address {}", address);
        return Err(ApiError::unauthorized("invalid SIWS signature"));
    }

    if let Some(exp) = parse_expiration_time(message) {
        let now = chrono::Utc::now();
        if exp < now {
            log::warn!("SIWS message expired for address {}", address);
            return Err(ApiError::unauthorized("SIWS message expired"));
        }
    }

    let pk_str = pubkey.to_string();
    let short = if pk_str.len() <= 10 {
        pk_str.clone()
    } else {
        format!("{}...{}", &pk_str[..6], &pk_str[pk_str.len() - 4..])
    };
    log::info!("SIWS verified pubkey={}", short);
    Ok(pk_str)
}

/// Parses "Expiration Time: <ISO 8601>" from SIWS-style message. Returns None if not present.
fn parse_expiration_time(message: &str) -> Option<chrono::DateTime<chrono::Utc>> {
    for line in message.lines() {
        let line = line.trim();
        if line.starts_with("Expiration Time:") {
            let rest = line.trim_start_matches("Expiration Time:").trim();
            return chrono::DateTime::parse_from_rfc3339(rest)
                .ok()
                .map(|dt| dt.with_timezone(&chrono::Utc));
        }
    }
    None
}

#[async_trait]
impl<S> FromRequestParts<S> for AuthUser
where
    S: Send + Sync,
{
    type Rejection = (StatusCode, Json<serde_json::Value>);

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        let headers = &parts.headers;
        let address = headers
            .get("x-siws-address")
            .and_then(|v| v.to_str().ok())
            .unwrap_or("")
            .to_string();
        let message_base64 = headers
            .get("x-siws-message")
            .and_then(|v| v.to_str().ok())
            .unwrap_or("")
            .to_string();
        let message = decode_siws_message_header(&message_base64).unwrap_or_default();
        let signature = headers
            .get("x-siws-signature")
            .and_then(|v| v.to_str().ok())
            .unwrap_or("")
            .to_string();

        if address.is_empty() || message_base64.is_empty() || signature.is_empty() {
            log::warn!("SIWS missing headers: address={} message_len={} signature_len={}", address.is_empty(), message_base64.is_empty(), signature.is_empty());
        }
        match verify_siws(&message, &signature, &address) {
            Ok(pubkey) => Ok(AuthUser { pubkey }),
            Err(e) => {
                log::warn!("SIWS rejected: {}", e.message);
                let body = serde_json::json!({ "error": e.message });
                Err((e.status, Json(body)))
            }
        }
    }
}
