package com.citahuellas.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Fichero cifrado con un JSON por objeto; escritura atómica vía fichero temporal. */
internal class EncryptedJsonFile(private val file: File, private val crypto: Crypto) {
    fun read(): String? = if (file.exists()) crypto.decrypt(file.readBytes()).decodeToString() else null

    fun write(json: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(crypto.encrypt(json.encodeToByteArray()))
        if (!tmp.renameTo(file)) {
            file.delete(); tmp.renameTo(file)
        }
    }
}

class ProfileStore(file: File, crypto: Crypto) {
    private val storage = EncryptedJsonFile(file, crypto)
    private val serializer = ListSerializer(Profile.serializer())

    @Synchronized
    fun load(): List<Profile> = storage.read()?.let { Json.decodeFromString(serializer, it) } ?: emptyList()

    @Synchronized
    fun save(list: List<Profile>) = storage.write(Json.encodeToString(serializer, list))

    @Synchronized
    fun upsert(p: Profile) {
        val list = load()
        save(if (list.any { it.id == p.id }) list.map { if (it.id == p.id) p else it } else list + p)
    }

    @Synchronized
    fun delete(id: String) = save(load().filterNot { it.id == id })
}
