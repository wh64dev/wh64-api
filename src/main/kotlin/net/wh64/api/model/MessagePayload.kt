package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    override val ok: Int = 1,
    override val status: Int = 200,
    val id: String,
    val addr: String,
    val message: String
) : GeneralPrinter
