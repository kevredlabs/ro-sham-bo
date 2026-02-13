//! Configuration loaded from environment variables.
//! Single place to validate and document required env vars.

pub struct Config {
    pub mongodb_uri: String,
    pub db_name: String,
}

impl Config {
    /// Load config from environment. Panics if required vars are missing.
    pub fn from_env() -> Self {
        Self {
            mongodb_uri: std::env::var("MONGODB_URI").expect("MONGODB_URI must be set"),
            db_name: std::env::var("MONGODB_DB_NAME").expect("MONGODB_DB_NAME must be set"),
        }
    }
}
