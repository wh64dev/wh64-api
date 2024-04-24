package net.wh64.api.api

import io.ktor.utils.io.charsets.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.wh64.api.model.HanRiverData
import net.wh64.api.util.unwrapQuote
import java.lang.String.format
import java.net.URI

class HanRiver(area: Int) {
    private val rawURL = "http://openapi.seoul.go.kr:8088/sample/json/WPOSInformationTime"

    private val url = URI.create(format("%s/%d/%d", rawURL, area, area))
    private val obj = Json.parseToJsonElement(url.toURL().readText(Charset.forName("UTF-8")))
    private val data = obj.jsonObject["WPOSInformationTime"]!!.jsonObject["row"]!!.jsonArray[0]
    private val datetime = format(
        "%sT%s:00+09:00",
        unwrapQuote(data.jsonObject["MSR_DATE"].toString().let { raw ->
            var str = ""
            for (i in 0 until 4) {
                str += raw[i]
            }
            str += "-"

            for (i in 4 until 6) {
                str += raw[i]
            }

            for (i in 6 until 8) {
                str += raw[i]
            }

            return@let str
        }),
        unwrapQuote(data.jsonObject["MSR_TIME"].toString())
    )

    fun build(): HanRiverData {
        return HanRiverData(
            area = unwrapQuote(data.jsonObject["SITE_ID"].toString()),
            datetime = datetime,
            temp = unwrapQuote(data.jsonObject["W_TEMP"].toString()).toDouble(),
            ph = unwrapQuote(data.jsonObject["W_PH"].toString()).toDouble()
        )
    }
}
