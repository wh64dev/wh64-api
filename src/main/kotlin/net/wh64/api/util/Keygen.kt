package net.wh64.api.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import net.wh64.api.Config
import net.wh64.api.service.AccountInfo
import net.wh64.api.service.AuthService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object Keygen {
    private val secret = Config.jwt_secret
    private val issuer = Config.jwt_issuer
    private val audience = Config.jwt_audience
    private val algorithm = Algorithm.HMAC256(secret)

    private const val VALIDITY = 7200000L

    private fun genExpired(): Date {
        return Date(System.currentTimeMillis() + VALIDITY)
    }

    fun verifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    suspend fun token(auth: AuthService, acc: AccountInfo): String {
        val issued = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS))
        auth.updateIssued(UUID.fromString(acc.id), issued)

        return JWT.create()
            .withSubject("WH64 API Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("email", acc.email)
            .withClaim("user_id", acc.id)
            .withClaim("username", acc.username)
            .withClaim("verified", acc.verified)
            .withIssuedAt(issued)
            .withExpiresAt(genExpired())
            .sign(algorithm)
    }
}
