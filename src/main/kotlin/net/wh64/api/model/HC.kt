package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class HC(
    override val ok: Int = 1,
    override val status: Int = 200,
    val response_time: String
) : GeneralPrinter
