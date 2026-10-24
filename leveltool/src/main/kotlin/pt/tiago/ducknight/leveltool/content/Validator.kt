package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.BOT_VERSION
import pt.tiago.ducknight.core.CONTENT_VERSION
import pt.tiago.ducknight.core.SIM_VERSION
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import java.nio.file.Files
import kotlin.io.path.exists

object Validator {
    fun validate(store: ContentStore, strict: Boolean = false): List<String> {
        val errors = mutableListOf<String>()
        if (!store.catalogPath.exists()) return listOf("catalog.json missing")
        val catalog = runCatching { store.loadCatalog() }.getOrElse {
            return listOf("catalog.json is not valid: ${it.message}")
        }
        if (catalog.simVersion != SIM_VERSION) errors += "catalog simVersion ${catalog.simVersion} != $SIM_VERSION"
        if (catalog.botVersion != BOT_VERSION) errors += "catalog botVersion ${catalog.botVersion} != $BOT_VERSION"
        if (catalog.contentVersion != CONTENT_VERSION) {
            errors += "catalog contentVersion ${catalog.contentVersion} != $CONTENT_VERSION"
        }
        for ((relative, expected) in catalog.files) {
            val path = store.root.resolve(relative)
            if (!path.exists()) {
                errors += "catalog file missing: $relative"
            } else if (JsonFiles.sha256(path) != expected) {
                errors += "catalog hash mismatch: $relative"
            }
        }
        val speciesIds = runCatching { store.readSpecies().map { it.id }.toSet() }
            .getOrElse {
                errors += "species.json invalid: ${it.message}"
                emptySet()
            }
        val families = runCatching {
            JsonFiles.readValue<FamilyCatalog>(store.templatesDir.resolve("families.json"))
                .families.map { it.id }.toSet()
        }.getOrElse {
            errors += "families.json invalid: ${it.message}"
            emptySet()
        }
        val levels = runCatching { store.levelFiles() }.getOrElse {
            errors += "levels invalid: ${it.message}"
            emptyList()
        }
        val ids = mutableSetOf<String>()
        for (level in levels) {
            if (!level.id.matches(Regex("p\\d{2}l\\d{3}"))) errors += "${level.id}: bad id"
            if (!ids.add(level.id)) errors += "${level.id}: duplicate id"
            if (level.place !in 1..12) errors += "${level.id}: place ${level.place}"
            if (level.ordinal !in 1..50) errors += "${level.id}: ordinal ${level.ordinal}"
            if (level.family !in families) errors += "${level.id}: unknown family ${level.family}"
            if (level.variants.keys != setOf("EASY", "MEDIUM", "HARD")) {
                errors += "${level.id}: variants ${level.variants.keys}"
            }
            for (difficulty in Difficulty.entries) {
                val variant = level.variants[difficulty.name] ?: continue
                if (variant.species.any { it.id !in speciesIds }) {
                    errors += "${level.id}-${difficulty.name}: unknown species"
                }
                if (variant.need > variant.birds && level.type != pt.tiago.ducknight.core.level.LevelType.BOSS) {
                    errors += "${level.id}-${difficulty.name}: need > birds"
                }
                if (variant.stars.two >= variant.stars.three) {
                    errors += "${level.id}-${difficulty.name}: star thresholds"
                }
                val spec = runCatching { level.spec(difficulty) }.getOrElse {
                    errors += "${level.id}-${difficulty.name}: ${it.message}"
                    null
                }
                if (spec != null && spec.type != level.type) {
                    errors += "${level.id}-${difficulty.name}: type mismatch"
                }
                if (strict) {
                    val certPath = store.certificatePath(level.id, difficulty)
                    if (!certPath.exists()) errors += "${level.id}-${difficulty.name}: certificate missing"
                }
            }
        }
        for (certificate in runCatching { store.loadCertificates() }.getOrElse {
            errors += "certificates invalid: ${it.message}"
            emptyList()
        }) {
            val difficulty = runCatching { Difficulty.valueOf(certificate.difficulty) }.getOrNull()
            if (difficulty == null) {
                errors += "${certificate.levelId}: bad certificate difficulty"
                continue
            }
            val level = levels.firstOrNull { it.id == certificate.levelId }
            if (level == null) {
                errors += "${certificate.levelId}: certificate for a missing level"
                continue
            }
            val spec = runCatching { level.spec(difficulty) }.getOrNull()
            if (spec == null) {
                errors += "${certificate.levelId}-${certificate.difficulty}: missing variant"
                continue
            }
            val hash = JsonFiles.sha256(JsonFiles.encodeValue(spec).toByteArray(Charsets.UTF_8))
            if (hash != certificate.variantHash) {
                errors += "${certificate.levelId}-${certificate.difficulty}: stale certificate"
            }
        }
        return errors
    }
}
