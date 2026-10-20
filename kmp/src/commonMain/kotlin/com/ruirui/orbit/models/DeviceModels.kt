package com.ruirui.orbit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceToken(
    val fid: String?,
    @SerialName("apns_token")
    val apnsToken: String?
)