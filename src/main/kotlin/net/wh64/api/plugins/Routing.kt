package net.wh64.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.wh64.api.Config
import net.wh64.api.service.DatabaseHealthCheck
import net.wh64.api.model.ErrorPrinter
import net.wh64.api.model.HealthCheck
import net.wh64.api.model.MessagePayload
import net.wh64.api.model.ResultPrinter
import net.wh64.api.service.SendService
import org.jetbrains.exposed.sql.Database
import java.util.*
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
    val sendService = SendService(database)

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

    authentication {
        jwt {
            realm = Config.jwt_realms
            verifier(
                JWT
                    .require(Algorithm.HMAC256(Config.jwt_secret))
                    .withAudience(Config.jwt_audience)
                    .withIssuer(Config.jwt_issuer)
                    .build()
            )

            validate { credential ->
                if (credential.payload.audience.contains(Config.jwt_audience)) JWTPrincipal(credential.payload) else null
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorPrinter(
                        status = HttpStatusCode.Unauthorized.value,
                        errno = "token is not invalid or has expired"
                    )
                )
            }
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

                val data = sendService.send(UUID.randomUUID(), call.request.origin.remoteAddress, nickname, message)
                call.respond(
                    HttpStatusCode.OK,
                    ResultPrinter(
                        response_time = "${System.currentTimeMillis() - start}ms",
                        data = mapOf("payload_id" to data.id)
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
