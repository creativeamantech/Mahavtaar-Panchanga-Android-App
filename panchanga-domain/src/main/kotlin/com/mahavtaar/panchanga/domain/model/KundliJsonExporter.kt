package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KundliJsonExporter {
    private val jsonCodec = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun toJson(report: KundliReport): String = jsonCodec.encodeToString(report)

    fun fromJson(raw: String): Result<KundliReport> = runCatching {
        jsonCodec.decodeFromString(KundliReport.serializer(), raw)
    }

    fun decodeBirthInput(raw: String): Result<KundliBirthInput> = runCatching {
        runCatching { jsonCodec.decodeFromString(KundliReport.serializer(), raw).input }.getOrElse {
            jsonCodec.decodeFromString(KundliBirthInput.serializer(), raw)
        }
    }
}
