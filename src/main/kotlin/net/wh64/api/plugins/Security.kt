package net.wh64.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import net.wh64.api.Config
import net.wh64.api.model.ErrorPrinter

fun Application.configureSecurity() {
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
}
