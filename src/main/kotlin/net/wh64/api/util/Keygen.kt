package net.wh64.api.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import net.wh64.api.Config
import net.wh64.api.service.Account
import java.util.*

object Keygen {
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
            .withAudience(audience)
            .build()
    }

    fun token(acc: Account): String {
        return JWT.create()
            .withSubject("WH64 API Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("email", acc.email)
            .withClaim("user_id", acc.id.toString())
            .withClaim("username", acc.username)
            .withExpiresAt(getExpired())
            .sign(algorithm)
    }
}
