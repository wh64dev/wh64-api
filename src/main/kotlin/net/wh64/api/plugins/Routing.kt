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
import net.wh64.api.model.*
import net.wh64.api.service.*
import net.wh64.api.util.Keygen
import net.wh64.api.util.database
import org.apache.commons.mail.SimpleEmail
import java.util.*
import javax.naming.AuthenticationException
import kotlin.time.Duration.Companion.seconds

private const val REPO_URL = "https://github.com/wh64dev/wh64-api.git"

fun Application.configureRouting() {
    val emailVerifier = EmailVerifyService(database)
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
            call.respond(
                HttpStatusCode.Unauthorized, ErrorPrinter(
                    status = HttpStatusCode.Unauthorized.value,
                    errno = cause.toString()
                )
            )
        }
    }

    authentication {
        jwt("auth") {
            realm = Config.jwt_realms
            verifier(Keygen.verifier())

            validate { credential ->
                val contain = credential.payload.audience.contains(Config.jwt_audience)
                val id = UUID.fromString(credential.payload.getClaim("user_id").asString().replace("\"", ""))
                val acc = auth.find(id)
                val exist = acc != null

                return@validate if (contain && exist) {
                    val data = JWTPrincipal(credential.payload)
                    if (acc?.lastLogin != null) {
                        val issued = data.payload.issuedAt.time
                        if (acc.lastLogin > issued) {
                            return@validate null
                        }
                    }

                    data
                } else {
                    null
                }
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

                val data = sendService.send(UUID.randomUUID(), call.request.origin.remoteAddress, nickname, message)
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

            route("/auth") {
                post("/login") {
                    val start = System.currentTimeMillis()
                    val form = call.receiveParameters()

                    val username = form["username"] ?: throw BadRequestException("`username` parameter must not be null")
                    val password = form["password"] ?: throw BadRequestException("`password` parameter must not be null")

                    val data = AuthData(username, password)
                    val res = auth.find(data) ?: throw AuthenticationException("username or password not matches")
                    val token = Keygen.token(auth, res)

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
                    val username = form["username"] ?: throw BadRequestException("`username` parameter must not be null")
                    val password = form["password"] ?: throw BadRequestException("`password` parameter must not be null")
                    val email = form["email"] ?: throw BadRequestException("`email` parameter must not be null")

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
                        val userId = principal!!.payload.getClaim("user_id").asString()
                        val account = auth.find(UUID.fromString(userId))
                        val token = Keygen.token(auth, account!!)

                        call.respond(
                            ResultPrinter(
                                response_time = "${System.currentTimeMillis() - start}ms",
                                data = Refresher(
                                    account = account,
                                    refresh_token = token
                                )
                            )
                        )
                    }

                    put("/verify") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = UUID.fromString(principal!!.payload.getClaim("user_id").asString())
                        val account = auth.find(userId)

                        if (account!!.verified) {
                            return@put call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "your account is already verified"
                                )
                            )
                        }

                        if (emailVerifier.find(userId)) {
                            return@put call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "verify code already sent your email, please try again later"
                                )
                            )
                        }

                        val code = emailVerifier.create(account)
                        val email = SimpleEmail()
                        email.apply {
                            hostName = Config.email_hostname
                            setSmtpPort(Config.email_smtp_port.toInt())
                            setAuthentication(Config.email_username, Config.email_password)
                            isSSLOnConnect = Config.email_is_ssl.toBoolean()
                            setFrom(Config.sender_email)
                            subject = "WH64 API: Account Verifier"
                            setMsg("Your verification code is: $code")
                            addTo(account.email)
                        }

                        email.send()
                        call.respond(
                            HttpStatusCode.OK, HC(response_time = "${System.currentTimeMillis() - start}ms")
                        )
                    }

                    post("/verify") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = UUID.fromString(principal!!.payload.getClaim("user_id").asString())
                        val form = call.receiveParameters()
                        val code = form["code"] ?: throw BadRequestException("`code` parameter must not be null")

                        val result = try {
                            emailVerifier.verify(userId, code)
                        } catch (ex: NotImplementedError) {
                            return@post call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "verify code not matches"
                                )
                            )
                        }

                        if (!result) {
                            return@post call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "verify code is expired"
                                )
                            )
                        }

                        emailVerifier.delete(userId)
                        auth.verify(userId)

                        call.respond(
                            HttpStatusCode.OK, ResultPrinter(
                                response_time = "${System.currentTimeMillis() - start}ms",
                                data = mapOf("request_id" to userId.toString())
                            )
                        )
                    }

                    patch("/edit") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getClaim("user_id").asString()
                        val form = call.receiveParameters()
                        val rawActType = form["action"] ?: throw BadRequestException("`action` parameter must not be null")
                        val actType = try {
                            AccEditType.valueOf(rawActType.uppercase())
                        } catch (ex: Exception) {
                            throw BadRequestException("invalid action type: $rawActType")
                        }

                        suspend fun <T> action(content: T) {
                            auth.edit(actType, UUID.fromString(userId), content)

                            call.respond(
                                HttpStatusCode.OK, ResultPrinter(
                                    response_time = "${System.currentTimeMillis() - start}ms",
                                    data = AccountAction(
                                        id = userId.toString(),
                                        action = "edit"
                                    )
                                )
                            )
                        }

                        when (actType) {
                            AccEditType.EMAIL -> {
                                val email = form["email"] ?: throw BadRequestException("`email` parameter must not be null")
                                action(email)
                            }

                            AccEditType.PASSWORD -> {
                                val password = form["password"] ?: throw BadRequestException("`password` parameter must not be null")
                                action(password)
                            }
                        }
                    }

                    delete("/unregister") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getClaim("user_id").asString()

                        auth.delete(UUID.fromString(userId))
                        call.respond(
                            HttpStatusCode.OK, ResultPrinter(
                                response_time = "${System.currentTimeMillis() - start}ms",
                                data = AccountAction(
                                    id = userId.toString(),
                                    action = "delete"
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
