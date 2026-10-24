package pt.tiago.ducknight.data

import pt.tiago.ducknight.core.replay.SavedLevel
import pt.tiago.ducknight.core.replay.SavedLevelCodec
import java.io.File
import java.io.FileOutputStream

/**
 * Atomic save of the active level. Primary file, backup file and fsync; a corrupt primary is
 * recovered from the backup or discarded so a bad write never blocks the game.
 */
class SavedLevelStore(private val directory: File) {
    private val primary = File(directory, "saved-level.json")
    private val backup = File(directory, "saved-level.bak.json")

    fun hasSaved(): Boolean = primary.isFile || backup.isFile

    fun save(saved: SavedLevel) {
        directory.mkdirs()
        val text = SavedLevelCodec.encode(saved)
        if (primary.isFile) {
            primary.copyTo(backup, overwrite = true)
        }
        val tmp = File(directory, "saved-level.tmp")
        FileOutputStream(tmp).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
        if (!tmp.renameTo(primary)) {
            primary.delete()
            check(tmp.renameTo(primary)) { "Could not replace the saved level" }
        }
        primary.copyTo(backup, overwrite = true)
    }

    fun load(): SavedLevel? {
        val fromPrimary = read(primary)
        if (fromPrimary != null) return fromPrimary
        val fromBackup = read(backup)
        if (fromBackup != null) {
            runCatching { save(fromBackup) }
            return fromBackup
        }
        clear()
        return null
    }

    fun clear() {
        primary.delete()
        backup.delete()
    }

    private fun read(file: File): SavedLevel? {
        if (!file.isFile) return null
        return runCatching { SavedLevelCodec.decode(file.readText(Charsets.UTF_8)) }.getOrNull()
    }
}
