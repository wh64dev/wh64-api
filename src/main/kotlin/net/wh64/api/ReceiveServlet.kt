package net.wh64.api

import com.google.gson.Gson
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import net.wh64.api.model.Error
import net.wh64.api.model.MessageLog
import net.wh64.api.model.MessagePayload
import net.wh64.api.model.check.SendCheck
import net.wh64.api.util.DatabaseUtil
import org.jetbrains.exposed.sql.insert

@WebServlet(name = "receiveServlet", value = ["/v1/recv"])
class ReceiveServlet : HttpServlet() {
    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val start = System.currentTimeMillis()
        val gson = Gson()
        res.characterEncoding = "UTF-8"
        res.contentType = "application/json"

        fun except() {
            res.status = HttpServletResponse.SC_BAD_REQUEST

            res.writer.use { out ->
                val raw = Error(0, res.status, "parameter cannot be empty")
                out.write(gson.toJson(raw))
            }
        }

        val id = req.getParameter("id") ?: return except()
        val addr = req.getHeader("X-Forwarded-For") ?: req.remoteAddr
        val message = req.getParameter("message") ?: return except()
        val database = DatabaseUtil()
        val payload: MessagePayload = try {
            runBlocking {
                database.dbQuery {
                    MessageLog.insert {
                        it[this.id] = id
                        it[this.addr] = addr
                        it[this.message] = message
                    }

                    return@dbQuery MessagePayload(id, addr, message)
                }
            }
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

        database.close()
    }
}