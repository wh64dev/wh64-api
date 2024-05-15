package net.wh64.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import net.wh64.api.Config
import net.wh64.api.model.ErrorPrinter
import java.util.*
import javax.naming.AuthenticationException

fun Application.configureSecurity() {
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorPrinter(
                status = HttpStatusCode.Unauthorized.value,
                errno = cause.toString()
            ))
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
                val contain = credential.payload.audience.contains(Config.jwt_audience)
                val id = UUID.fromString(credential.payload.getClaim("user_id").asString().replace("\"", ""))

                if (credential.payload.audience.contains(Config.jwt_audience)) JWTPrincipal(credential.payload) else null
            }

            challenge { _, _ ->
                throw AuthenticationException("token is not invalid or has expired")
            }
        }
    }
}
