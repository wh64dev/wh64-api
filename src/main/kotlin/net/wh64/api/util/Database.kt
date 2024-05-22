package net.wh64.api.util

import net.wh64.api.Config
import org.jetbrains.exposed.sql.Database

val database = Database.connect(
    url = "jdbc:mariadb://${Config.db_url}/${Config.db_name}",
    driver = "org.mariadb.jdbc.Driver",
    user = Config.db_username,
    password = Config.db_password
)
