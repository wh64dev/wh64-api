package net.wh64.api.plugins

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
import net.wh64.api.model.ErrorPrinter
import net.wh64.api.model.HealthCheck
import net.wh64.api.model.ResultPrinter
import net.wh64.api.service.*
import net.wh64.api.util.Keygen
import net.wh64.api.util.database
import java.util.*
import javax.naming.AuthenticationException
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

private const val REPO_URL = "https://github.com/wh64dev/wh64-api.git"

fun Application.configureRouting() {
    val healthCheck = DatabaseHealthCheck(database)
    val sendService = SendService(database)
    val auth = AuthService(database)

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

        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorPrinter(
                status = HttpStatusCode.Unauthorized.value,
                errno = cause.toString()
            ))
        }
    }

    authentication {
        jwt("auth") {
            realm = Config.jwt_realms
            verifier(Keygen.verifier())

            validate { credential ->
                val contain = credential.payload.audience.contains(Config.jwt_audience)
                val id = UUID.fromString(credential.payload.getClaim("user_id").asString().replace("\"", ""))
                val exist = auth.find(id) != null

                if (contain && exist) JWTPrincipal(credential.payload) else null
            }

            challenge { _, _ ->
                throw AuthenticationException("token is not invalid or has expired")
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

            route("/auth") {
                post("/login") {
                    val start = System.currentTimeMillis()
                    val form = call.receiveParameters()

                    val username = form["username"] ?: throw Exception("`username` parameter must not be null")
                    val password = form["password"] ?: throw Exception("`password` parameter must not be null")

                    val data = AuthData(username, password)
                    val res = auth.find(data) ?: throw AuthenticationException("username or password not matches")
                    val token = Keygen.token(res)

                    call.respond(
                        HttpStatusCode.OK, ResultPrinter(
                            data = token,
                            response_time = "${System.currentTimeMillis() - start}ms"
                        )
                    )
                }

                put("/register") {
                    val start = System.currentTimeMillis()
                    val form = call.receiveParameters()
                    val username = form["username"] ?: throw Exception("`username` parameter must not be null")
                    val password = form["password"] ?: throw Exception("`password` parameter must not be null")
                    val checkPW = form["password_check"] ?: throw Exception("`password_check` parameter must not be null")
                    val email = form["email"] ?: throw Exception("`email` parameter must not be null")

                    if (password != checkPW) {
                        throw BadRequestException("`password` parameter must match password_check")
                    }

                    val acc = Account(
                        id = UUID.randomUUID(),
                        username = username,
                        password = password,
                        email = email
                    )

                    val id = auth.create(acc)
                    call.respond(
                        HttpStatusCode.OK, ResultPrinter(
                            data = id.toString(),
                            response_time = "${System.currentTimeMillis() - start}ms"
                        )
                    )
                }

                authenticate("auth") {
                    get {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()

                        call.respond(mapOf("hello" to "world", "respond_time" to "${System.currentTimeMillis() - start}ms"))
                    }
                }
            }
        }
    }
}
