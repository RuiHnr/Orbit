use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Register Request sent from user to Server
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct RegisterRequest {
    pub username: String,
    pub identity_key_dh: String,
    pub identity_key_sign: String,
}

/// The Register Response sent from the server
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct RegisterResponse {
    pub user_id: Uuid,
    pub token: String,
}

/// Data payload inside JWT token
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct Claims {
    pub sub: Uuid,  // Subject: The user's UUID
    pub exp: usize, // Expiration time (Unix timestamp)
}

/// Request sent by client to check for username availability
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct CheckUsernameRequest {
    pub username: String,
}

/// Response sent back to client informing whether username is available
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct CheckUsernameResponse {
    pub available: bool,
}
