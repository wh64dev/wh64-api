package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheck(
    val id: Int,
    val transactionId: String,
    val request_timestamp: String
)