package net.wh64.api

import com.google.gson.Gson
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import net.wh64.api.model.check.HealthCheck
import net.wh64.api.util.DatabaseUtil
import org.jetbrains.exposed.sql.SchemaUtils

@WebServlet(name = "api", value = ["/v1", "/v1/"])
class APIServlet : HttpServlet() {
    private val database = DatabaseUtil()

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val start = System.currentTimeMillis()
        res.contentType = "application/json"
        res.status = HttpServletResponse.SC_OK
        res.characterEncoding = Charsets.UTF_8.name()
        runBlocking {
            database.dbQuery {
                return@dbQuery SchemaUtils.listTables()
            }
        }

        val gson = Gson()
        res.writer.use { out ->
            val raw = HealthCheck(1, res.status, "${System.currentTimeMillis() - start}ms")
            out.write(gson.toJson(raw))
        }
    }
}