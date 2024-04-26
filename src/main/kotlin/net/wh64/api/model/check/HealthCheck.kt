package net.wh64.api.model.check

data class HealthCheck(
    val ok: Int,
    val status: Int,
    val version: String,
    val respond_time: String
)
