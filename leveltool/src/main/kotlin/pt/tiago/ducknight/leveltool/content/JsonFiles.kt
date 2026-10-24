package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Canonical JSON for the published catalog: object keys sorted recursively, two spaces,
 * UTF-8 without BOM and a final newline. Two runs of the tool must produce the same bytes.
 */
object JsonFiles {
    val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun canonical(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.entries.sortedBy { it.key }.associate { (key, value) -> key to canonical(value) },
        )
        is JsonArray -> JsonArray(element.map { canonical(it) })
        else -> element
    }

    fun encode(element: JsonElement): String =
        json.encodeToString(JsonElement.serializer(), canonical(element)) + "\n"

    inline fun <reified T> encodeValue(value: T): String =
        encode(json.encodeToJsonElement(kotlinx.serialization.serializer<T>(), value))

    fun write(path: Path, element: JsonElement) {
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, encode(element), StandardCharsets.UTF_8)
    }

    inline fun <reified T> writeValue(path: Path, value: T) {
        write(path, json.encodeToJsonElement(kotlinx.serialization.serializer<T>(), value))
    }

    fun read(path: Path): JsonElement = json.parseToJsonElement(Files.readString(path, StandardCharsets.UTF_8))

    inline fun <reified T> readValue(path: Path): T =
        json.decodeFromString(kotlinx.serialization.serializer<T>(), Files.readString(path, StandardCharsets.UTF_8))

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256(path: Path): String = sha256(Files.readAllBytes(path))
}
