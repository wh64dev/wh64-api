package net.wh64.api.service

import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import net.wh64.api.Config
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class AuthData(
    val username: String,
    val password: String
)

data class Account(
    val id: UUID,
    val username: String,
    val password: String,
    val email: String
)

data class AccountInfo(
    val id: String,
    val username: String,
    val email: String
)

class AuthService(database: Database) {
    object AuthTable : Table("account") {
        val id = uuid("id")
        val username = varchar("username", 50).uniqueIndex()
        val password = varchar("password", 100)
        val salt = varchar("salt", 100)
        val email = varchar("email", 100).uniqueIndex()

        override val primaryKey = PrimaryKey(id, name = "PK_account_id")
    }

    init {
        transaction(database) {
            SchemaUtils.create(AuthTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun salt(username: String): String = dbQuery {
        AuthTable.select(AuthTable.salt).where(AuthTable.username.eq(username)).single()[AuthTable.salt]
    }

    private fun hashToCount(password: String, salt: String): String {
        var hashed = hash("$password:$salt")

        for (i in 0 until Config.hash_count) {
            hashed = hash("$hashed:$salt")
        }

        return hashed
    }

    suspend fun create(data: Account): UUID = dbQuery {
        val salt = SecureRandom().let {
            val bytes = ByteArray(Config.salt_size)
            it.nextBytes(bytes)

            bytes.encodeBase64()
        }

        AuthTable.insert {
            it[this.id] = data.id
            it[this.username] = data.username
            it[this.password] = hashToCount(data.password, salt)
            it[this.salt] = salt
            it[this.email] = data.email
        }[AuthTable.id]
    }

    suspend fun find(data: AuthData): Account? = dbQuery {
        val salt = try {
            salt(data.username)
        } catch (_: Exception) {
            return@dbQuery null
        }

        AuthTable.select(AuthTable.id, AuthTable.username, AuthTable.password, AuthTable.email)
            .where { AuthTable.username.eq(data.username) and AuthTable.password.eq(hashToCount(data.password, salt)) }
            .map {
                Account(
                    id = it[AuthTable.id],
                    username = it[AuthTable.username],
                    password = it[AuthTable.password],
                    email = it[AuthTable.email]
                )
            }
            .singleOrNull()
    }

    suspend fun find(id: UUID): Account? = dbQuery {
        AuthTable.select(AuthTable.id, AuthTable.username, AuthTable.password, AuthTable.email)
            .where { AuthTable.id.eq(id) }
            .map {
                Account(
                    id = it[AuthTable.id],
                    username = it[AuthTable.username],
                    password = it[AuthTable.password],
                    email = it[AuthTable.email]
                )
            }
            .singleOrNull()
    }

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        private fun hash(str: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return Base64.encode(md.digest(str.toByteArray()))
        }
    }
}