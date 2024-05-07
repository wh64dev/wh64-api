package net.wh64.api.service.model

import kotlinx.serialization.Serializable

@Serializable
data class ExposedUser(val name: String, val age: Int)
