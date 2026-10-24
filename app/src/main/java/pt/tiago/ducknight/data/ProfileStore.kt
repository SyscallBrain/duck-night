package pt.tiago.ducknight.data

import kotlinx.serialization.json.Json
import pt.tiago.ducknight.progress.Profile
import java.io.File
import java.io.FileOutputStream

/**
 * Atomic profile storage: JSON, `.tmp` + fsync + rename, and a `.bak` copy. Export and import are
 * plain JSON so the player can move the file through the system file picker.
 */
class ProfileStore(private val directory: File) {
    private val file = File(directory, "profile.json")
    private val backup = File(directory, "profile.bak.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun hasProfile(): Boolean = file.isFile || backup.isFile

    fun load(): Profile {
        val primary = read(file)
        if (primary != null) return primary
        val fromBackup = read(backup)
        if (fromBackup != null) {
            runCatching { save(fromBackup) }
            return fromBackup
        }
        return Profile()
    }

    fun save(profile: Profile) {
        directory.mkdirs()
        val text = json.encodeToString(Profile.serializer(), profile)
        if (file.isFile) file.copyTo(backup, overwrite = true)
        val tmp = File(directory, "profile.tmp")
        FileOutputStream(tmp).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not replace the profile" }
        }
        file.copyTo(backup, overwrite = true)
    }

    fun export(profile: Profile): String = json.encodeToString(Profile.serializer(), profile)

    fun import(text: String): Profile? = runCatching {
        json.decodeFromString(Profile.serializer(), text).sanitized()
    }.getOrNull()

    fun clear() {
        file.delete()
        backup.delete()
    }

    private fun read(source: File): Profile? {
        if (!source.isFile) return null
        return runCatching {
            json.decodeFromString(Profile.serializer(), source.readText(Charsets.UTF_8)).sanitized()
        }.getOrNull()
    }
}
