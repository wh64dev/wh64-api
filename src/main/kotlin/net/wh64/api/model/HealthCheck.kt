package net.wh64.api.model

data class HealthCheck(
    val ok: Int,
    val status: Int,
    val respond_time: String
)
