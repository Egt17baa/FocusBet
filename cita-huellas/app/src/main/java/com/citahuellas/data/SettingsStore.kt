package com.citahuellas.data

import kotlinx.serialization.json.Json
import java.io.File

/** Ajustes cifrados igual que los perfiles (incluyen teléfono y correo propios). */
class SettingsStore(file: File, crypto: Crypto) {
    private val storage = EncryptedJsonFile(file, crypto)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppSettings =
        (storage.read()?.let { json.decodeFromString(AppSettings.serializer(), it) } ?: AppSettings()).clamped()

    fun save(s: AppSettings) = storage.write(json.encodeToString(AppSettings.serializer(), s.clamped()))
}
