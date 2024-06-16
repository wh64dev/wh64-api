package net.wh64.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountAction(
    val id: String,
    val action: String
)
