package net.wh64.api.service

import kotlinx.coroutines.Dispatchers
import net.wh64.api.model.HealthCheck
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHealthCheck(database: Database) {
    object HealthCheckTable : Table("health_check") {
        val id = integer("id").autoIncrement()
        val responseTime = long("response_time")
        val timestamp = long("request_timestamp")
        val transactionId = varchar("transaction_id", 36)

        override val primaryKey = PrimaryKey(id, name = "PK_HealthCheck_ID")
    }

    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    init {
        transaction(database) {
            SchemaUtils.create(HealthCheckTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun insert(start: Long): Int = dbQuery {
        HealthCheckTable.insert {
            it[timestamp] = System.currentTimeMillis()
            it[responseTime] = System.currentTimeMillis() - start
            it[transactionId] = UUID.randomUUID().toString()
        }[HealthCheckTable.id]
    }

    suspend fun count(): Int = dbQuery {
        HealthCheckTable.select(HealthCheckTable.id).count().toInt()
    }

    suspend fun query(size: Int = 5): List<HealthCheck> = dbQuery {
        HealthCheckTable.selectAll().orderBy(HealthCheckTable.timestamp, SortOrder.DESC).limit(size).map {
            HealthCheck(
                it[HealthCheckTable.id],
                "${it[HealthCheckTable.responseTime]}ms",
                it[HealthCheckTable.transactionId],
                format.format(it[HealthCheckTable.timestamp])
            )
        }
    }

    suspend fun queryPage(page: Int, size: Int = 5): List<HealthCheck> = dbQuery {
        HealthCheckTable.selectAll()
            .orderBy(HealthCheckTable.timestamp, SortOrder.DESC)
            .limit(size, (size * (page - 1).toLong())).map {
                HealthCheck(
                    it[HealthCheckTable.id],
                    "${it[HealthCheckTable.responseTime]}ms",
                    it[HealthCheckTable.transactionId],
                    format.format(it[HealthCheckTable.timestamp])
                )
        }
    }
}
