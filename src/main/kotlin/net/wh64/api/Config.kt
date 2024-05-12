package net.wh64.api

import org.jetbrains.exposed.sql.exposedLogger
import java.io.File
import java.nio.charset.Charset
import java.util.Properties
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

object Config {
    private fun <T> useConfig(): ConfigDelegate<T> {
        return ConfigDelegate()
    }

    private fun <T> useProject(): DefaultDelegate<T> {
        return DefaultDelegate()
    }

    val db_url: String by useConfig()
    val db_name: String by useConfig()
    val db_username: String by useConfig()
    val db_password: String by useConfig()

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
