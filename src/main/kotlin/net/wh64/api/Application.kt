package net.wh64.api

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.wh64.api.plugins.configureHTTP
import net.wh64.api.plugins.configureRouting
import net.wh64.api.plugins.configureSecurity
import net.wh64.api.plugins.configureStatic

fun main() {
    embeddedServer(Netty, port = Config.port.toInt(), host = Config.host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureStatic()
    configureRouting()
    configureSecurity()
}
