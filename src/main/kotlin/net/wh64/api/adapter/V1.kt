package net.wh64.api.adapter

import io.ktor.server.routing.*
import net.wh64.api.service.AuthService
import net.wh64.api.service.EmailVerifyService
import net.wh64.api.service.SendService
import net.wh64.api.util.database

data class ServicePack(
    val auth: AuthService,
    val send: SendService,
    val emailVerifier: EmailVerifyService
)

fun Route.v1(build: Route.(ServicePack) -> Unit) = route("/v1") {
    val packed = ServicePack(
        auth = AuthService(database),
        send = SendService(database),
        emailVerifier = EmailVerifyService(database)
    )

    build(packed)
}