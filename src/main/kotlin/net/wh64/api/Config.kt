package net.wh64.api

import java.io.File
import java.util.*
import kotlin.reflect.KProperty

object Config {
    private fun <T> useConfig(): ConfigDelegate<T> {
        return ConfigDelegate()
    }

    private fun <T> useProject(): DefaultDelegate<T> {
        return DefaultDelegate()
    }

    val host: String by useConfig()
    val port: String by useConfig()

    val db_url: String by useConfig()
    val db_name: String by useConfig()
    val db_username: String by useConfig()
    val db_password: String by useConfig()

    val jwt_secret: String by useConfig()
    val jwt_issuer: String by useConfig()
    val jwt_realms: String by useConfig()
    val jwt_audience: String by useConfig()

    val sender_email: String by useConfig()
    val email_hostname: String by useConfig()
    val email_smtp_port: String by useConfig()
    val email_username: String by useConfig()
    val email_password: String by useConfig()
    val email_is_ssl: String by useConfig()

    val salt_size: String by useConfig()
    val hash_count: String by useConfig()

    val version: String by useProject()
}

@Suppress("UNCHECKED_CAST")
private class ConfigDelegate<T> : DelegateGenerator<T> {
    override val props = Properties()
    override fun getValue(thisRef: Any, property: KProperty<*>): T = props[property.name] as T

    init {
        val file = File("./config.properties")
        if (!file.exists()) {
            val stream = javaClass.getResourceAsStream("/config.properties")!!
            stream.use { buf ->
                val buffer = buf.readAllBytes()

                file.createNewFile()
                file.writeBytes(buffer)
            }

            throw NullPointerException("config not found in your service directory. please edit `config.properties` file first.")
        }

        props.load(file.inputStream())
    }
}

@Suppress("UNCHECKED_CAST")
private class DefaultDelegate<T> : DelegateGenerator<T> {
    override val props = Properties()
    override fun getValue(thisRef: Any, property: KProperty<*>): T = props[property.name] as T

    init {
        props.load(javaClass.getResourceAsStream("/setting.properties"))
    }
}

private interface DelegateGenerator<T> {
    val props: Properties
    operator fun getValue(thisRef: Any, property: KProperty<*>): T
}
