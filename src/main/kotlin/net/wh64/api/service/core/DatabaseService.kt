package net.wh64.api.service.core

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

open class DatabaseService(database: Database, table: Table?) {
    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    init {
        transaction(database) {
            if (table == null) {
                return@transaction
            }

            SchemaUtils.create(table)
        }
    }
}