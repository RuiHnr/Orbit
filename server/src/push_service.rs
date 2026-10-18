use a2::{
    Client as ApnsClient, ClientConfig, DefaultNotificationBuilder, Endpoint, NotificationBuilder,
    NotificationOptions, Priority, PushType,
};
use gcp_auth::{CustomServiceAccount, TokenProvider};
use reqwest::Client as HttpClient;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::env;
use std::fs::File;
use std::sync::Arc;

/// The two modes the client operates in based on whether any friend is watching
#[derive(Serialize, Deserialize, Debug, Clone, Copy, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum SyncMode {
    High,
    Low,
}

/// Manages HTTP/2 connection pools to Google FCM and Apple APNs.
/// This should be instantiated once at startup and wrapped in an Arc
pub struct PushService {
    fcm_client: HttpClient,
    fcm_service_account: Option<Arc<CustomServiceAccount>>,
    fcm_project_id: Option<String>,
    apns_client: Option<ApnsClient>,
}

impl PushService {
    /// Initializes the push service, reading credentials from env and/or service account file.
    pub fn new() -> Result<Self, Box<dyn std::error::Error>> {
        // 1. Initialize APNs (Optional)
        let apns_client = Self::init_apns();

        // 2. Initialize FCM (Optional)
        let (fcm_service_account, fcm_project_id) = Self::init_fcm();

        if apns_client.is_none() && fcm_service_account.is_none() {
            return Err(
                "No push notification credentials found (neither APNs nor FCM configured)".into(),
            );
        }

        Ok(Self {
            fcm_client: HttpClient::new(),
            fcm_service_account,
            fcm_project_id,
            apns_client,
        })
    }

    fn init_apns() -> Option<ApnsClient> {
        let team_id = env::var("APNS_TEAM_ID").ok()?;
        let key_id = env::var("APNS_KEY_ID").ok()?;
        let p8_path = env::var("APNS_KEY_PATH").unwrap_or_else(|_| "AuthKey.p8".to_string());

        let apns_file = match File::open(&p8_path) {
            Ok(file) => file,
            Err(e) => {
                eprintln!(
                    "⚠️ APNs key file '{}' not found: {}. APNs disabled.",
                    p8_path, e
                );
                return None;
            }
        };

        match ApnsClient::token(
            apns_file,
            key_id,
            team_id,
            ClientConfig {
                endpoint: Endpoint::Production,
                ..ClientConfig::default()
            },
        ) {
            Ok(client) => {
                println!("✅ APNs client initialized successfully!");
                Some(client)
            }
            Err(e) => {
                eprintln!("⚠️ Failed to initialize APNs client: {}. APNs disabled.", e);
                None
            }
        }
    }

    fn init_fcm() -> (Option<Arc<CustomServiceAccount>>, Option<String>) {
        let creds_path = env::var("FIREBASE_CREDENTIALS_PATH")
            .unwrap_or_else(|_| "firebase-credentials.json".to_string());

        let mut project_id = None;

        let service_account = match CustomServiceAccount::from_file(&creds_path) {
            Ok(sa) => {
                println!("✅ FCM credentials loaded from '{}'", creds_path);
                if let Ok(file_content) = std::fs::read_to_string(&creds_path)
                    && let Ok(json_val) = serde_json::from_str::<serde_json::Value>(&file_content)
                    && let Some(pid) = json_val.get("project_id").and_then(|v| v.as_str())
                {
                    project_id = Some(pid.to_string());
                }

                Some(Arc::new(sa))
            }
            Err(e) => {
                eprintln!(
                    "ℹ️ FCM service account file '{}' not found: {}.",
                    creds_path, e
                );
                None
            }
        };

        (service_account, project_id)
    }

    pub async fn send_fcm_sync_action(
        &self,
        fcm_token: &str,
        mode: SyncMode,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let (Some(project_id), Some(sa)) = (&self.fcm_project_id, &self.fcm_service_account) else {
            return Err("FCM service account or project ID not configured".into());
        };

        let token = sa
            .token(&["https://www.googleapis.com/auth/firebase.messaging"])
            .await?;
        let access_token = token.as_str();

        let url = format!(
            "https://fcm.googleapis.com/v1/projects/{}/messages:send",
            project_id
        );

        let payload = json!({
            "message": {
                "token": fcm_token,
                "data": { "action": "sync", "mode": mode },
                "android": { "priority": "high" },
                "apns": {
                    "headers": { "apns-push-type": "background", "apns-priority": "5" },
                    "payload": { "aps": { "content-available": 1 } }
                }
            }
        });

        self.fcm_client
            .post(&url)
            .bearer_auth(access_token)
            .json(&payload)
            .send()
            .await?
            .error_for_status()?;

        Ok(())
    }

    pub async fn send_apns_sync_action(
        &self,
        apns_token: &str,
        mode: SyncMode,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let Some(apns_client) = &self.apns_client else {
            return Ok(()); // APNs not configured, silently skip
        };

        // Apple strictly requires Priority::Normal and PushType::Background for silent pushes
        let options = NotificationOptions {
            apns_push_type: Some(PushType::Background),
            apns_priority: Some(Priority::Normal),
            ..Default::default()
        };

        // Setting content-available to 1 is required for a background wake-up
        let mut payload = DefaultNotificationBuilder::new()
            .set_content_available()
            .build(apns_token, options);

        payload.add_custom_data("action", &"sync")?;
        payload.add_custom_data("mode", &mode)?;

        apns_client.send(payload).await?;
        Ok(())
    }
}
