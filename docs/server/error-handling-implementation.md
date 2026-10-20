# Structured Error Handling Implementation Guide

## Overview
This document outlines steps to implement consistent, scalable error handling in the Orbit Rust backend (Axum framework). The goal is to return machine-readable error codes and human-friendly messages while maintaining security and observability.

## Step 1: Define Error Types

1. Create `server/src/errors.rs`:
```rust
use axum::http::StatusCode;
use thiserror::Error;
use uuid::Uuid;

#[derive(Debug, Error)]
pub enum AppError {
    #[error("User not found: {0}")]
    UserNotFound(Uuid),

    #[error("Invalid credentials")]
    InvalidCredentials,

    #[error("Validation failed: {0:?}")]
    ValidationFailed(Vec<String>),

    #[error("Database error")]
    DatabaseError(#[from] sqlx::Error),

    #[error("Rate limit exceeded")]
    RateLimitExceeded,

    #[error("Internal server error")]
    InternalServerError,
}

// Map AppError to HTTP status codes
impl AppError {
    pub fn status_code(&self) -> StatusCode {
        match self {
            AppError::UserNotFound(_) => StatusCode::NOT_FOUND,
            AppError::InvalidCredentials => StatusCode::UNAUTHORIZED,
            AppError::ValidationFailed(_) => StatusCode::BAD_REQUEST,
            AppError::DatabaseError(_) => StatusCode::INTERNAL_SERVER_ERROR,
            AppError::RateLimitExceeded => StatusCode::TOO_MANY_REQUESTS,
            AppError::InternalServerError => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}

// Convert AppError to axum response
impl IntoResponse for AppError {
    fn into_response(self) -> axum::response::Response {
        let error_code = match self {
            AppError::UserNotFound(_) => "USER_NOT_FOUND",
            AppError::InvalidCredentials => "INVALID_CREDENTIALS",
            AppError::ValidationFailed(_) => "VALIDATION_FAILED",
            AppError::DatabaseError(_) => "DATABASE_ERROR",
            AppError::RateLimitExceeded => "RATE_LIMIT_EXCEEDED",
            AppError::InternalServerError => "INTERNAL_ERROR",
        };

        let status = self.status_code();
        let message = self.to_string();

        // In production, omit sensitive details from 'details'
        let details = if cfg!(debug_assertions) {
            Some(message)
        } else {
            None
        };

        (status, Json(ErrorResponse {
            code: error_code.to_string(),
            message: message,
            details,
        })).into_response()
    }
}

#[derive(serde::Serialize)]
struct ErrorResponse {
    code: String,
    message: String,
    details: Option<String>,
}
```

## Step 2: Update Handlers

Modify endpoint handlers to return `Result<T, AppError>`:

**Before** (in `handlers/users.rs`):
```rust
pub async fn get_user(
    Path(user_id): Path<Uuid>,
    State(pool): State<PgPool>,
) -> impl IntoResponse {
    let user = sqlx::query_as!(...)
        .fetch_one(&pool)
        .await
        .expect("User not found");
    // ...
}
```

**After**:
```rust
pub async fn get_user(
    Path(user_id): Path<Uuid>,
    State(pool): State<PgPool>,
) -> Result<Json<UserResponse>, AppError> {
    let user = sqlx::query_as!(...)
        .fetch_one(&pool)
        .await
        .map_err(|e| match e {
            sqlx::Error::RowNotFound => AppError::UserNotFound(user_id),
            _ => AppError::DatabaseError(e),
        })?;
    Ok(Json(user.into()))
}
```

## Step 3: Central Error Handling

In `lib.rs`, add global error handling middleware:
```rust
use axum::{middleware::Next, response::Response};

async fn handle_errors<B>(
    request: Request<B>,
    next: Next<B>,
) -> Result<Response, AppError> {
    Ok(next.run(request).await)
}

// In your router setup
let app = Router::new()
    .route("/users", get(get_user))
    .layer(TraceLayer::new_for_http())
    .layer(axum::middleware::from_fn(handle_errors));
```

## Step 4: Update Validation

Modify input validation to collect errors:
```rust
// Example: User registration validation
fn validate_user(input: &UserInput) -> Result<(), AppError> {
    let mut errors = Vec::new();

    if !is_valid_email(&input.email) {
        errors.push("Invalid email format".to_string());
    }
    if input.password.len() < 8 {
        errors.push("Password too short (min 8 chars)".to_string());
    }

    if !errors.is_empty() {
        return Err(AppError::ValidationFailed(errors));
    }
    Ok(())
}
```

## Step 5: Rate Limiting

Add rate limiting middleware (example using `tower-governor`):
```rust
use tower_governor::{Governor, GovernorConfig};

let governor = Governor::new(GovernorConfig::default());

let app = Router::new()
    .layer(governor)
    .route("/inbox", post(submit_location))
    .with_state(pool);
```

Handle rate limit errors:
```rust
async fn submit_location(
    // ...
) -> Result<impl IntoResponse, AppError> {
    let remaining = check_rate_limit(user_id).await?;
    if remaining <= 0 {
        return Err(AppError::RateLimitExceeded);
    }
    // ...
}
```

## Step 6: Logging

Add structured logging in handlers:
```rust
use tracing::error;

// In any handler returning AppError
.map_err(|e| {
    error!(%user_id, "Request failed: {:?}", e);
    e
})?;
```

## Step 7: Update Tests

Modify test cases to validate error responses:
```rust
#[tokio::test]
async fn test_user_not_found() {
    let response = get_user(Uuid::nil).await;
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    let body: ErrorResponse = response.json().await;
    assert_eq!(body.code, "USER_NOT_FOUND");
}
```

## Step 8: Deploy & Monitor

1. Monitor new error types in production
2. Add error budget alerts for `INTERNAL_ERROR`
3. Update API documentation with error examples

## Migration Plan

1. **Phase 1**: Implement for new endpoints
2. **Phase 2**: Refactor high-traffic endpoints (`/inbox`, `/friends`)
3. **Phase 3**: Update remaining endpoints

> **Security Note**: Never include stack traces or database snippets in production error details.
