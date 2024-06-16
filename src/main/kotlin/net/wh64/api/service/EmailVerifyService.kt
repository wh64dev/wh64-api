package net.wh64.api.service

import net.wh64.api.util.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.random.Random

private data class VerifyData(
    val code: String,
    val expired: Long
)

class EmailVerifyService(database: Database) {
    object EmailVerifyTable : Table("verification_email") {
        val id = uuid("id").references(AuthService.AuthTable.id, onDelete = ReferenceOption.CASCADE)
        val email = varchar("email", 100)
        val code = varchar("code", 6)
        val expired = long("expired")

        override val primaryKey = PrimaryKey(id, name = "PK_verification_email_id")
    }

    init {
        transaction(database) {
            SchemaUtils.create(EmailVerifyTable)
        }
    }

    suspend fun create(info: AccountInfo): String = dbQuery {
        fun rand(): String {
            return Random.nextInt(0, 10).toString()
        }

        var secretCode = ""
        for (i in 0..5) {
            secretCode += rand()
        }

        println(secretCode)

        EmailVerifyTable.insert {
            it[id] = UUID.fromString(info.id)
            it[email] = info.email
            it[expired] = System.currentTimeMillis() + 180000
            it[code] = secretCode
        }[EmailVerifyTable.code]
    }

    suspend fun find(id: UUID): Boolean = dbQuery {
        !EmailVerifyTable.select(EmailVerifyTable.id).where { EmailVerifyTable.id eq id }.empty()
    }

    suspend fun verify(id: UUID, code: String): Boolean = dbQuery {
        val saved = EmailVerifyTable.select(EmailVerifyTable.expired, EmailVerifyTable.code).where {
                EmailVerifyTable.id eq id
            }.map {
            VerifyData(it[EmailVerifyTable.code], it[EmailVerifyTable.expired])
        }.single()

        if (saved.expired < System.currentTimeMillis()) {
            delete(id)
            return@dbQuery false
        }

        return@dbQuery code == saved.code
    }

    suspend fun delete(id: UUID) = dbQuery {
        EmailVerifyTable.deleteWhere { this.id eq id }
    }
}