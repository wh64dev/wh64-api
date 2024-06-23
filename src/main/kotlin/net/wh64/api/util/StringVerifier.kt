package net.wh64.api.util

fun String.verifyStr(): Boolean = Regex("^[a-zA-Z0-9\\-_]+").matches(this)
