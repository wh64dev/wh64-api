package net.wh64.api.service

import io.ktor.util.*
import kotlinx.serialization.Serializable
import net.wh64.api.Config
import net.wh64.api.util.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

@Serializable
data class AccountInfo(
    val id: String,
    val username: String,
    val email: String,
    val created: Long,
    val lastLogin: Long?,
    val verified: Boolean
)

enum class AccEditType {
    EMAIL,
    PASSWORD;
}

class AuthService(database: Database) {
    object AuthTable : Table("account") {
        val id = uuid("id")
        val username = varchar("username", 50).uniqueIndex()
        val password = varchar("password", 100)
        val salt = varchar("salt", 100)
        val email = varchar("email", 100).uniqueIndex()
        val created = long("created")
        val lastLogin = long("last_login").nullable()
        val verified = bool("verified").default(false)

        override val primaryKey = PrimaryKey(id, name = "PK_account_id")
    }

    init {
        transaction(database) {
            SchemaUtils.create(AuthTable)
        }
    }

    private fun genSalt() = SecureRandom().let {
        val bytes = ByteArray(Config.salt_size.toInt())
        it.nextBytes(bytes)

        bytes.encodeBase64()
    }

    private suspend fun salt(username: String): String = dbQuery {
        AuthTable.select(AuthTable.salt).where(AuthTable.username.eq(username)).single()[AuthTable.salt]
    }

    private fun hashToCount(password: String, salt: String): String {
        var hashed = hash("$password:$salt")

        for (i in 0 until Config.hash_count.toInt()) {
            hashed = hash("$hashed:$salt")
        }

        return hashed
    }

    suspend fun create(data: Account): UUID = dbQuery {
        val salt = genSalt()
        AuthTable.insert {
            it[this.id] = data.id
            it[this.username] = data.username
            it[this.password] = hashToCount(data.password, salt)
            it[this.created] = System.currentTimeMillis()
            it[this.salt] = salt
            it[this.email] = data.email
        }[AuthTable.id]
    }

    suspend fun find(data: AuthData): AccountInfo? = dbQuery {
        val salt = try {
            salt(data.username)
        } catch (_: Exception) {
            return@dbQuery null
        }

        AuthTable.selectAll()
            .where { AuthTable.username.eq(data.username) and AuthTable.password.eq(hashToCount(data.password, salt)) }
            .map {
                AccountInfo(
                    id = it[AuthTable.id].toString(),
                    username = it[AuthTable.username],
                    email = it[AuthTable.email],
                    created = it[AuthTable.created],
                    lastLogin = it[AuthTable.lastLogin],
                    verified = it[AuthTable.verified]
                )
            }
            .singleOrNull()
    }

    suspend fun find(id: UUID): AccountInfo? = dbQuery {
        AuthTable.selectAll()
            .where { AuthTable.id.eq(id) }
            .map {
                AccountInfo(
                    id = it[AuthTable.id].toString(),
                    username = it[AuthTable.username],
                    email = it[AuthTable.email],
                    created = it[AuthTable.created],
                    lastLogin = it[AuthTable.lastLogin],
                    verified = it[AuthTable.verified]
                )
            }
            .singleOrNull()
    }

    suspend fun <T> edit(type: AccEditType, id: UUID, content: T) = dbQuery {
        AuthTable.update({ AuthTable.id eq id }) {
            when (type) {
                AccEditType.EMAIL -> {
                    it[this.email] = content.toString()
                    it[this.verified] = false
                }

                AccEditType.PASSWORD -> {
                    val salt = genSalt()

                    it[this.password] = hashToCount(content.toString(), salt)
                    it[this.salt] = salt
                }
            }
        }
    }

    suspend fun updateIssued(id: UUID, date: Date) = dbQuery {
        AuthTable.update({ AuthTable.id eq id }) {
            it[lastLogin] = date.time
        }
    }

    suspend fun verify(id: UUID) = dbQuery {
        AuthTable.update({ AuthTable.id eq id }) {
            it[verified] = true
        }
    }

    suspend fun delete(id: UUID) = dbQuery {
        AuthTable.deleteWhere { AuthTable.id eq id }
    }

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        private fun hash(str: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return Base64.encode(md.digest(str.toByteArray()))
        }
    }
}