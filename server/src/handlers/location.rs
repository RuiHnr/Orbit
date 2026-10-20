use crate::error::IntoStatusCode;
use crate::models::{SubmitLocationRequest, SubmitLocationResponse};
use crate::push_service::SyncMode;
use crate::{AppState, db};
use axum::{
    Extension, Json,
    extract::State,
    http::{HeaderValue, StatusCode},
    response::IntoResponse,
};
use uuid::Uuid;

/// Receives an encrypted location payload and queues it in the receiver's inbox.
/// Only allowed if the sender and receiver are confirmed friends.
pub(crate) async fn receive_location(
    State(state): State<AppState>,
    Extension(authenticated_user): Extension<Uuid>,
    Json(payloads): Json<Vec<SubmitLocationRequest>>,
) -> Result<impl IntoResponse, StatusCode> {
    let mut accepted = 0;

    for payload in &payloads {
        // Only insert if sender and receiver are confirmed friends
        let are_friends = db::are_friends(&state.db, authenticated_user, payload.receiver_id)
            .await
            .or_500()?;

        if are_friends {
            db::insert_location(
                &state.db,
                &authenticated_user,
                &payload.receiver_id,
                &payload.encrypted_blob,
            )
            .await
            .or_500()?;
            accepted += 1;
        }
    }

    let tokens = db::device_token::get_device_token(&state.db, authenticated_user)
        .await
        .or_500()?
        .expect("device token not found");
    if let Some(push_service) = &state.push_service {
        if let Some(fcm) = tokens.fid
            && let Err(e) = push_service
                .send_fcm_sync_action(&fcm, SyncMode::High)
                .await
        {
            eprintln!("FCM push failed: {}", e);
        }
        if let Some(apns) = tokens.apns_token
            && let Err(e) = push_service
                .send_apns_sync_action(&apns, SyncMode::High)
                .await
        {
            eprintln!("APNS push failed: {}", e);
        }
    }

    Ok((StatusCode::OK, Json(SubmitLocationResponse { accepted })).into_response())
}

/// Retrieves and consumes all pending location messages for the authenticated user.
/// Messages are permanently deleted from the server once fetched.
/// Also returns the number of available one time keys left
pub(crate) async fn get_inbox(
    State(state): State<AppState>,
    Extension(authenticated_user): Extension<Uuid>,
) -> Result<impl IntoResponse, StatusCode> {
    let messages = db::fetch_inbox(&state.db, authenticated_user)
        .await
        .or_500()?;

    let remaining_keys = db::count_onetime_prekeys(&state.db, authenticated_user)
        .await
        .unwrap_or(0);

    let mut response = (StatusCode::OK, Json(messages)).into_response();
    response.headers_mut().insert(
        "X-Remaining-PreKeys",
        HeaderValue::from_str(&remaining_keys.to_string()).unwrap(),
    );

    Ok(response)
}
