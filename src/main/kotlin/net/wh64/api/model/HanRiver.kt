package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class HanRiverResp(
    val ok: Int = 1,
    val status: Int = 200,
    val area_code: Int = 2,
    val data: HanRiverData,
    val response_time: String = "0ms"
)

@Serializable
data class HanRiverData(
    val area: String,
    val datetime: String,
    val ph: Double,
    val temp: Double
)
