package net.wh64.api

import com.google.gson.Gson
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import net.wh64.api.model.Error
import net.wh64.api.model.MessagePayload
import net.wh64.api.model.check.SendCheck
import net.wh64.api.service.SendService
import net.wh64.api.util.DBConnection
import java.util.*

@WebServlet(name = "sendServlet", value = ["/v1/send", "/v1/send/"])
class SendServlet : HttpServlet() {
    private val database = DBConnection()
    private val gson = Gson()

    private fun except(res: HttpServletResponse, reason: String) {
        res.status = HttpServletResponse.SC_BAD_REQUEST

        res.writer.use { out ->
            val raw = Error(0, res.status, reason)
            out.write(gson.toJson(raw))
        }
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        res.addHeader("Access-Control-Allow-Origin", "*")

        val start = System.currentTimeMillis()
        res.characterEncoding = "UTF-8"
        res.contentType = "application/json"

        val id = UUID.randomUUID()
        val addr = req.getHeader("X-Forwarded-For") ?: req.remoteAddr
        val nickname = req.getParameter("nickname") ?: "Anonymous"
        val message = req.getParameter("message") ?: return except(res, "parameter cannot be empty")
        if (message.length > 100) {
            return except(res, "message parameter length cannot be greater than 100")
        }

        if (nickname.length > 20) {
            return except(res, "nickname parameter length cannot be greater than 20")
        }

        val service = SendService(database.open())
        val payload: MessagePayload = try {
            runBlocking { service.send(id, addr, nickname, message) }
        } catch (ex: Exception) {
            res.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            res.writer.use { out ->
                val raw = Error(0, res.status, ex.toString())
                out.write(gson.toJson(raw))
            }

            return
        }

        res.status = HttpServletResponse.SC_OK
        res.writer.use { out ->
            val raw = SendCheck(1, res.status, payload.id, "${System.currentTimeMillis() - start}ms")
            out.write(gson.toJson(raw))
            out.flush()
            out.close()
        }
    }

    override fun destroy() {
        database.close()
    }
}
