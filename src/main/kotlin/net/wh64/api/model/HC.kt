package net.wh64.api.model

import kotlinx.serialization.Serializable
import net.wh64.api.Config

@Serializable
data class HC(
    override val ok: Int = 1,
    override val status: Int = 200,
    val version: String = Config.version,
    val response_time: String
) : GeneralPrinter
