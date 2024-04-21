package net.wh64.api.model

data class HanRiverResp(
    val ok: Int,
    val status: Int,
    val area_code: Int,
    val data: HanRiverData,
    val respond_time: String
)

data class HanRiverData(
    val area: String,
    val datetime: String,
    val ph: Double,
    val temp: Double
)
