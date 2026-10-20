use serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, sqlx::FromRow)]
pub struct DeviceToken {
    pub fid: Option<String>,
    pub apns_token: Option<String>,
}
