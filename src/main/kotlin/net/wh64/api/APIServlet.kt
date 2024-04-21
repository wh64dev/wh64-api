package net.wh64.api

import com.google.gson.Gson
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.wh64.api.model.HealthCheck

@WebServlet(name = "api", value = ["/v1"])
class APIServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val start = System.currentTimeMillis()
        res.contentType = "application/json"
        res.status = HttpServletResponse.SC_OK
        res.characterEncoding = Charsets.UTF_8.name()

        val gson = Gson()
        res.writer.use { out ->
            val raw = HealthCheck(1, 200, "${System.currentTimeMillis() - start}ms")
            out.write(gson.toJson(raw))
        }
    }
}