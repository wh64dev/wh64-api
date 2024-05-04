package net.wh64.api.service

import net.wh64.api.model.MessagePayload
import net.wh64.api.service.core.DatabaseService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import java.util.UUID

class SendService(database: Database) : DatabaseService(database, MessageLog) {
    object MessageLog : Table("message_log") {
        val id = varchar("id", 36)
        val addr = varchar("addr", 15)
        val created = long("created")
        val message = text("message")

        override val primaryKey = PrimaryKey(id, name = "PK_MessageLog_ID")
    }

    suspend fun send(id: UUID, addr: String, message: String): MessagePayload = dbQuery {
        MessageLog.insert {
            it[this.id] = id.toString()
            it[this.addr] = addr
            it[this.message] = message
            it[this.created] = System.currentTimeMillis()
        }

        return@dbQuery MessagePayload(id.toString(), message, addr)
    }
}