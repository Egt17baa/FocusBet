package com.citahuellas.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val fullName: String,
    val nie: String,
    val country: String,
    val phone: String,
    val email: String,
    val note: String = "",
)

@Serializable
data class AppSettings(
    val intervalSec: Int = 60,
    val maxAttempts: Int = 10,
    val myPhone: String = "",
    val myEmail: String = "",
)

fun AppSettings.clamped(): AppSettings = copy(
    intervalSec = intervalSec.coerceIn(10, 300),
    maxAttempts = maxAttempts.coerceIn(1, 20),
)
