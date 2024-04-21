package net.wh64.api.model

import org.jetbrains.exposed.sql.Table

object MessageLog : Table("message_log") {
    val id = varchar("id", 36)
    val addr = varchar("addr", 15)
    val created = long("created")
    val message = text("message")

    override val primaryKey = PrimaryKey(id, name = "PK_MessageLog_ID")
}
