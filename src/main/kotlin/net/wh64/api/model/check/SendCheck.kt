package net.wh64.api.model.check

data class SendCheck(
    val ok: Int,
    val status: Int,
    val id: String,
    val respond_time: String
)
