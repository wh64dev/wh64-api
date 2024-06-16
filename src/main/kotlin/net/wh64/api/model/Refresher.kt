package net.wh64.api.model

import kotlinx.serialization.Serializable
import net.wh64.api.service.AccountInfo

@Serializable
class Refresher(
    val account: AccountInfo,
    val refresh_token: String
)

@Serializable
class TokenPrinter(val token: String)
