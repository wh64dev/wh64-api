package net.wh64.api.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.wh64.api.Config
import net.wh64.api.adapter.v1
import net.wh64.api.model.*
import net.wh64.api.service.HanRiverService
import java.util.*
import javax.naming.AuthenticationException
import kotlin.time.Duration.Companion.seconds

private const val REPO_URL = "https://github.com/wh64dev/wh64-api.git"

fun Application.configureRouting() {

    if (Config.inner_rate_limit.toBoolean()) {
        install(RateLimit) {
            global {
                rateLimiter(limit = 5, refillPeriod = 3.seconds)
            }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        statusFile(HttpStatusCode.NotFound, filePattern = "public/error/404.html")

        if (Config.inner_rate_limit.toBoolean()) {
            statusFile(HttpStatusCode.TooManyRequests, filePattern = "public/error/429.html")
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

        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized, ErrorPrinter(
                    status = HttpStatusCode.Unauthorized.value,
                    errno = cause.toString()
                )
            )
        }
    }

    routing {
        get {
            call.respondRedirect(Url(REPO_URL))
        }

        v1 { packed ->
            get {
                val start = System.currentTimeMillis()
                call.respond(
                    HttpStatusCode.OK,
                    HC(response_time = "${System.currentTimeMillis() - start}ms")
                )
            }

            post("/send") {
                val start = System.currentTimeMillis()
                val form = call.receiveParameters()

                val nickname = form["nickname"] ?: "Anonymous"
                val message = form["message"].toString()

                if (message.isBlank()) {
                    throw BadRequestException("`message` parameter cannot be null")
                }

                if (message.length > 100) {
                    throw BadRequestException("`message` parameter length cannot greater than 100 characters")
                }

                val data = packed.send.send(UUID.randomUUID(), call.request.origin.remoteAddress, nickname, message)
                call.respond(
                    HttpStatusCode.OK,
                    ResultPrinter(
                        response_time = "${System.currentTimeMillis() - start}ms",
                        data = mapOf("payload_id" to data.id)
                    )
                )
            }

            route("/hanriver") {
                get {
                    val start = System.currentTimeMillis()
                    val data = HanRiverService(2).build()

                    call.respond(
                        HttpStatusCode.OK,
                        HanRiverResp(
                            ok = if (data.temp == -1.0) 0 else 1,
                            data = data,
                            response_time = "${System.currentTimeMillis() - start}ms"
                        )
                    )
                }

                get("/{area}") {
                    val area = call.parameters["area"]!!

                    val start = System.currentTimeMillis()
                    val data = HanRiverService(area.toInt()).build()

                    call.respond(
                        HttpStatusCode.OK,
                        HanRiverResp(
                            ok = if (data.temp == -1.0) 0 else 1,
                            data = data,
                            response_time = "${System.currentTimeMillis() - start}ms"
                        )
                    )
                }
            }
        }
    }
}
