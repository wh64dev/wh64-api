package net.wh64.api.util

import kotlinx.coroutines.Dispatchers
import net.wh64.api.Config
import net.wh64.api.model.MessageLog
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseUtil {
    private val database = Database.connect(
        url = "jdbc:mariadb://${Config.database_url}/${Config.database_name}",
        driver = "org.mariadb.jdbc.Driver",
        user = Config.database_username,
        password = Config.database_password
    )

    init {
        transaction(database) {
            SchemaUtils.create(MessageLog)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    fun close() {
        database.connector.invoke().close()
    }
}
