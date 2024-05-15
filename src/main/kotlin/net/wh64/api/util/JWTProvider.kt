package net.wh64.api.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import net.wh64.api.Config
import net.wh64.api.model.User
import java.util.*

object JWTProvider {
    private val secret = Config.jwt_secret
    private val issuer = Config.jwt_issuer
    private val audience = Config.jwt_audience
    private val algorithm = Algorithm.HMAC256(secret)

    private const val VALIDITY = 7200000

    private fun getExpired(): Date {
        return Date(System.currentTimeMillis() + VALIDITY)
    }

    fun verifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .build()
    }

    fun genToken(user: User): String {
        return JWT.create()
            .withSubject("Project API Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("user_id", user.id.toString())
            .withClaim("username", user.username)
            .withExpiresAt(getExpired())
            .sign(algorithm)
    }
}
