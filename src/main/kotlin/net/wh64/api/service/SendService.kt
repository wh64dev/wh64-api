package net.wh64.api.service

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers
import net.wh64.api.model.MessagePayload
import org.jetbrains.exposed.sql.*
import java.util.*

class SendService(database: Database) {
    object MessageLog : Table("message_log") {
        val id = varchar("id", 36)
        val addr = varchar("addr", 15)
        val nickname = varchar("nickname", 20).default("Anonymous")
        val created = long("created")
        val message = text("message")

        override val primaryKey = PrimaryKey(id, name = "PK_MessageLog_ID")
    }

    init {
        transaction(database) {
            SchemaUtils.create(MessageLog)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun send(id: UUID, addr: String, nickname: String, message: String): MessagePayload = dbQuery {
        MessageLog.insert {
            it[this.id] = id.toString()
            it[this.addr] = addr
            it[this.nickname] = nickname
            it[this.message] = message
            it[this.created] = System.currentTimeMillis()
        }

        return@dbQuery MessagePayload(id.toString(), message, addr)
    }
}
