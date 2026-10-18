use crate::error::IntoStatusCode;
use crate::models::{
    CheckUsernameRequest, CheckUsernameResponse, Claims, RegisterRequest, RegisterResponse,
};
use crate::{AppState, db};
use axum::extract::Query;
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use chrono::{Duration, Utc};
use jsonwebtoken::{EncodingKey, Header, encode};

/// Registers a new user, saving their public key and returning a JWT token for authentication.
pub(crate) async fn register_user(
    State(state): State<AppState>,
    Json(payload): Json<RegisterRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = db::create_user(&state.db, &payload).await.map_err(|e| {
        // Check for PostgreSQL 23505 (unique_violation on username)
        if let sqlx::Error::Database(ref db_err) = e
            && db_err.code().as_deref() == Some("23505")
        {
            eprintln!(
                "Registration rejected: Username '{}' is already taken",
                payload.username
            );
            return StatusCode::CONFLICT; // 409
        }

        eprintln!("DB-Error while registering: {}", e);
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    println!(
        "Successfully registered user: {} (ID: {})",
        payload.username, user_id
    );

    // 1. Set expiration
    let expiration = Utc::now()
        .checked_add_signed(Duration::days(30))
        .expect("valid timestamp")
        .timestamp() as usize;

    let claims = Claims {
        sub: user_id,
        exp: expiration,
    };

    // 2. Encode the token
    let token = encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(state.jwt_secret.as_bytes()),
    )
    .or_500()?;

    Ok((StatusCode::OK, Json(RegisterResponse { user_id, token })).into_response())
}

pub(crate) async fn check_username(
    State(state): State<AppState>,
    Query(query): Query<CheckUsernameRequest>,
) -> Result<Json<CheckUsernameResponse>, StatusCode> {
    let username = query.username.trim();
    if username.is_empty() {
        return Err(StatusCode::BAD_REQUEST);
    }

    let existing_user = db::get_user_id_by_name(&state.db, username)
        .await
        .or_500()?;

    Ok(Json(CheckUsernameResponse {
        available: existing_user.is_none(),
    }))
}
