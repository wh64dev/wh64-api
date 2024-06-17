package net.wh64.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.wh64.api.Config
import net.wh64.api.adapter.v1
import net.wh64.api.model.*
import net.wh64.api.service.*
import net.wh64.api.util.Keygen
import net.wh64.api.util.database
import org.apache.commons.mail.SimpleEmail
import java.util.*
import javax.naming.AuthenticationException

fun Application.configureSecurity() {
    authentication {
        val auth = AuthService(database)

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
        v1 { packed ->
            route("/auth") {
                post("/login") {
                    val start = System.currentTimeMillis()
                    val form = call.receiveParameters()

                    val username = form["username"] ?: throw BadRequestException("`username` parameter must not be null")
                    val password = form["password"] ?: throw BadRequestException("`password` parameter must not be null")

                    val data = AuthData(username, password)
                    val res = packed.auth.find(data) ?: throw AuthenticationException("username or password not matches")
                    val token = Keygen.token(packed.auth, res)

                    call.respond(
                        HttpStatusCode.OK, ResultPrinter(
                            data = TokenPrinter(token),
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

                    val id = packed.auth.create(acc)
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
                        val account = packed.auth.find(UUID.fromString(userId))

                        call.respond(
                            ResultPrinter(
                                response_time = "${System.currentTimeMillis() - start}ms",
                                data = account
                            )
                        )
                    }

                    get("/refresh") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getClaim("user_id").asString()
                        val account = packed.auth.find(UUID.fromString(userId))
                        val token = Keygen.token(packed.auth, account!!)

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
                        val account = packed.auth.find(userId)

                        if (account!!.verified) {
                            return@put call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "your account is already verified"
                                )
                            )
                        }

                        if (packed.emailVerifier.find(userId)) {
                            return@put call.respond(
                                HttpStatusCode.Forbidden, ErrorPrinter(
                                    status = HttpStatusCode.Forbidden.value,
                                    errno = "verify code already sent your email, please try again later"
                                )
                            )
                        }

                        val code = packed.emailVerifier.create(account)
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
                            HttpStatusCode.OK, SimpleResult(response_time = "${System.currentTimeMillis() - start}ms")
                        )
                    }

                    post("/verify") {
                        val start = System.currentTimeMillis()
                        val principal = call.principal<JWTPrincipal>()
                        val userId = UUID.fromString(principal!!.payload.getClaim("user_id").asString())
                        val form = call.receiveParameters()
                        val code = form["code"] ?: throw BadRequestException("`code` parameter must not be null")

                        val result = try {
                            packed.emailVerifier.verify(userId, code)
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

                        packed.emailVerifier.delete(userId)
                        packed.auth.verify(userId)

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
                            packed.auth.edit(actType, UUID.fromString(userId), content)

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

                        packed.auth.delete(UUID.fromString(userId))
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
