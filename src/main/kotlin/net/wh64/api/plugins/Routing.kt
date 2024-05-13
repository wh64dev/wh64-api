package net.wh64.api.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.wh64.api.Config
import net.wh64.api.service.DatabaseHealthCheck
import net.wh64.api.model.ErrorPrinter
import net.wh64.api.model.HealthCheck
import net.wh64.api.model.ResultPrinter
import org.jetbrains.exposed.sql.Database
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

private const val REPO_URL = "https://github.com/wh64dev/wh64-api.git"

fun Application.configureRouting() {
    val database = Database.connect(
        url = "jdbc:mariadb://${Config.db_url}/${Config.db_name}",
        driver = "org.mariadb.jdbc.Driver",
        user = Config.db_username,
        password = Config.db_password
    )
    val healthCheck = DatabaseHealthCheck(database)

    install(RateLimit) {
        global {
            rateLimiter(limit = 5, refillPeriod = 3.seconds)
        }
    }
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(
                status,
                ErrorPrinter(
                    status = status.value,
                    errno = "You are throttled! Please wait 3 seconds."
                )
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorPrinter(
                    status = HttpStatusCode.BadRequest.value,
                    errno = cause.toString()
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorPrinter(errno = cause.toString()))
        }
    }

    routing {
        get {
            call.respondRedirect(Url(REPO_URL))
        }

        route("/v1") {
            get {
                val start = System.currentTimeMillis()
                val id = healthCheck.insert(start)

                call.respond(
                    HttpStatusCode.OK,
                    ResultPrinter(
                        response_time = "${System.currentTimeMillis() - start}ms",
                        data = mapOf("id" to id)
                    )
                )
            }

            get("/hc") {
                val start = System.currentTimeMillis()
                val default = 5

                val page = call.parameters["page"]?.toIntOrNull()
                val size = call.parameters["size"]?.toIntOrNull() ?: default

                if (healthCheck.count() == 0) {
                    return@get call.respond(
                        HttpStatusCode.OK,
                        ResultPrinter(
                            response_time = "${System.currentTimeMillis() - start}ms",
                            data = listOf<HealthCheck>()
                        )
                    )
                }

                if (page != null) {
                    if (page <= 0 || ceil(healthCheck.count().toDouble() / size.toDouble()).toInt() < page) {
                        throw BadRequestException("`page` parameter must 1~${ceil(healthCheck.count().toDouble() / size.toDouble()).toInt()}")
                    }
                }

                if (size <= 0 || size > healthCheck.count()) {
                    throw BadRequestException("`size` parameter must 1~${healthCheck.count()}")
                }

                val data = if (page == null) {
                    healthCheck.query(size)
                } else {
                    healthCheck.queryPage(page, size)
                }

                call.respond(
                    HttpStatusCode.OK,
                    ResultPrinter(
                        response_time = "${System.currentTimeMillis() - start}ms",
                        data = data
                    )
                )
            }
        }
    }
}
