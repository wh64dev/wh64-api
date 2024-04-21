package net.wh64.api

import com.google.gson.Gson
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.wh64.api.api.HanRiver
import net.wh64.api.model.Error
import net.wh64.api.model.HanRiverResp
import kotlin.random.Random

@WebServlet(name = "hanRiverServlet", urlPatterns = ["/v1/hanriver", "/v1/hanriver/"])
class HanRiverServlet : HttpServlet() {
    private val gson = Gson()

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val start = System.currentTimeMillis()
        res.contentType = "application/json"
        res.characterEncoding = "UTF-8"
        res.addHeader("Access-Control-Allow-Origin", "*")

        res.status = HttpServletResponse.SC_OK
        val area = Random.nextInt(1, 6)

        res.writer.write(gson.toJson(HanRiverResp(
            ok = 1,
            status = res.status,
            area_code = area,
            data = HanRiver(area).build(),
            respond_time = "${System.currentTimeMillis() - start}ms"
        )))

        res.writer.flush()
        res.writer.close()
    }
}

@WebServlet(name = "hanRiverNumberServlet", urlPatterns = ["/v1/hanriver/*"])
class HanRiverNumberServlet : HttpServlet() {
    private val gson = Gson()

    private fun except(res: HttpServletResponse) {
        res.status = HttpServletResponse.SC_BAD_REQUEST
        res.writer.use { out ->
            out.write(gson.toJson(Error(0, res.status, "area code must be integer")))
            out.flush()
            out.close()
        }
    }

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val start = System.currentTimeMillis()
        res.contentType = "application/json"
        res.characterEncoding = "UTF-8"
        res.addHeader("Access-Control-Allow-Origin", "*")

        val path = req.requestURI.substring(req.contextPath.length).split("/")
        val area = try {
            path[3].toInt()
        } catch (ex: NumberFormatException) {
            return except(res)
        } catch (ex: ArrayIndexOutOfBoundsException) {
            Random.nextInt(1, 6)
        }

        res.status = HttpServletResponse.SC_OK
        res.writer.use { out ->
            out.write(gson.toJson(HanRiverResp(
                ok = 1,
                status = res.status,
                area_code = area,
                data = HanRiver(area).build(),
                respond_time = "${System.currentTimeMillis() - start}ms"
            )))

            out.flush()
            out.close()
        }
    }
}
