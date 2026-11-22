package com.citahuellas.web

import com.citahuellas.engine.PageSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SnapshotDto(val url: String = "", val ids: List<String> = emptyList(), val text: String = "")

private val json = Json { ignoreUnknownKeys = true }

/** Convierte el resultado de `evaluateJavascript("CH.snapshot()")` (literal JSON de un string JSON). */
fun parseSnapshot(raw: String?): PageSnapshot? = runCatching {
    val inner = json.decodeFromString<String?>(raw ?: return null) ?: return null
    val dto = json.decodeFromString(SnapshotDto.serializer(), inner)
    PageSnapshot(dto.url, dto.ids.toSet(), dto.text)
}.getOrNull()
