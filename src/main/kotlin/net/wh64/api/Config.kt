package net.wh64.api

import java.util.Properties
import kotlin.reflect.KProperty

object Config {
    private fun <T> useConfig(): ConfigDelegate<T> {
        return ConfigDelegate()
    }

    val database_url by useConfig<String>()
    val database_name by useConfig<String>()
    val database_username by useConfig<String>()
    val database_password by useConfig<String>()
    val inline_allowed_cors by useConfig<String>()
}

@Suppress("UNCHECKED_CAST")
class ConfigDelegate<T> {
    private val props = Properties()

    init {
        props.load(this.javaClass.getResourceAsStream("/config.properties"))
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = props[property.name] as T
}
