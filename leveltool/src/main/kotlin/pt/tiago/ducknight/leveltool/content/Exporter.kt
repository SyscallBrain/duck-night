package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.BOT_VERSION
import pt.tiago.ducknight.core.CONTENT_VERSION
import pt.tiago.ducknight.core.SIM_VERSION
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object Exporter {
    data class Result(val files: Int, val bytes: Long, val milliseconds: Long)

    fun export(store: ContentStore, output: Path): Result {
        val errors = Validator.validate(store, strict = true)
        require(errors.isEmpty()) { "Catalog is not valid:\n" + errors.joinToString("\n") }
        val started = System.nanoTime()
        if (output.exists()) deleteRecursively(output)
        Files.createDirectories(output)
        val catalog = store.loadCatalog()
        val sources = mutableListOf(store.catalogPath)
        sources += catalog.files.keys.map { store.root.resolve(it) }
        var bytes = 0L
        val hashes = mutableMapOf<String, String>()
        for (source in sources.distinct().sortedBy { store.relative(it) }) {
            val relative = store.relative(source)
            val target = output.resolve(relative)
            Files.createDirectories(target.parent)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            bytes += Files.size(target)
            hashes[relative] = JsonFiles.sha256(source)
        }
        hashes["catalog.json"] = JsonFiles.sha256(store.catalogPath)
        JsonFiles.writeValue(
            output.resolve("manifest.json"),
            AssetManifest(SIM_VERSION, BOT_VERSION, CONTENT_VERSION, hashes.toSortedMap()),
        )
        bytes += Files.size(output.resolve("manifest.json"))
        val elapsed = (System.nanoTime() - started) / 1_000_000
        return Result(sources.size + 1, bytes, elapsed)
    }

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
