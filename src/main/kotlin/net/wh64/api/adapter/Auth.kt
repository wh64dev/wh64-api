package net.wh64.api.adapter

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.useAuth(build: () -> Route) = authenticate("auth") {
    build.invoke()
}