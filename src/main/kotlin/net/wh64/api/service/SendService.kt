package net.wh64.api.service

import net.wh64.api.model.MessagePayload
import net.wh64.api.util.dbQuery
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class SendService(database: Database) {
    object MessageLog : Table("message") {
        val id = uuid("id")
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

    suspend fun send(id: UUID, addr: String, nickname: String, message: String): MessagePayload = dbQuery {
        MessageLog.insert {
            it[this.id] = id
            it[this.addr] = addr
            it[this.nickname] = nickname
            it[this.message] = message
            it[this.created] = System.currentTimeMillis()
        }

        return@dbQuery MessagePayload(id = id.toString(), message = message, addr = addr)
    }
}
