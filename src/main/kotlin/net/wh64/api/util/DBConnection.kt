package net.wh64.api.util

import net.wh64.api.Config
import org.jetbrains.exposed.sql.Database

class DBConnection {
    private val database = Database.connect(
        url = "jdbc:mariadb://${Config.database_url}/${Config.database_name}",
        driver = "org.mariadb.jdbc.Driver",
        user = Config.database_username,
        password = Config.database_password
    )

    fun open(): Database {
        return database
    }

    fun close() {
        database.connector.invoke().close()
    }
}
