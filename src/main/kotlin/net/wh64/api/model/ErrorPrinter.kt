package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorPrinter(
    override val ok: Int = 0,
    override val status: Int = 500,
    val errno: String
) : GeneralPrinter
