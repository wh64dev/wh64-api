package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultPrinter<T>(
    override val ok: Int = 1,
    override val status: Int = 200,
    val response_time: String = "0ms",
    val data: T?
) : GeneralPrinter

@Serializable
data class SimpleResult(
    override val ok: Int = 1,
    override val status: Int = 200,
    val response_time: String
) : GeneralPrinter
