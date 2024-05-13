package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheck(
    val id: Int,
    val response_time: String,
    val transaction_id: String,
    val request_timestamp: String
)
