package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.random.SplitMix64

object DailyPoolGenerator {
    fun generate(store: ContentStore, version: Int): DailyPool {
        val certificates = store.loadCertificates()
        require(certificates.isNotEmpty()) { "No certificates: run certify first" }
        val variants = certificates.map { DailyRound(it.levelId, it.difficulty) }
            .distinctBy { it.levelId to it.difficulty }
        require(variants.size >= 3) { "At least three certified variants are needed" }
        val rng = SplitMix64(0xD411_7A11L + version)
        val entries = (0 until 365).map {
            val rounds = (0 until 3).map {
                variants[rng.nextInt(variants.size)]
            }
            DailyEntry(rounds)
        }
        val pool = DailyPool(version = version, entries = entries)
        store.writeDailyPool(pool)
        store.updateCatalog()
        return pool
    }
}
