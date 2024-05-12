package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(val id: String, val addr: String, val message: String)
