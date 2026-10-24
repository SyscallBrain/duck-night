package pt.tiago.ducknight.catalog

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import pt.tiago.ducknight.core.content.Catalog
import pt.tiago.ducknight.core.content.DailyPool
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.content.PlaceFile
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.species.Species

/**
 * Loads the catalog exported by :leveltool from the APK assets. Nothing is fetched from the
 * network and the files are checked against the catalog list at load time.
 */
class ContentRepository(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }

    val catalog: Catalog = read("catalog.json")
    val species: List<Species> = read("species.json")
    val places: List<Int> = catalog.places
    val levels: List<LevelFile> = catalog.levelIds.map { id -> read("levels/p${id.substring(1, 3)}/$id.json") }
    val dailyPool: DailyPool = read("daily/pool.json")

    private val speciesById: Map<String, Species> = species.associateBy { it.id }
    private val levelsById: Map<String, LevelFile> = levels.associateBy { it.id }

    fun level(id: String): LevelFile? = levelsById[id]

    fun levelsForPlace(place: Int): List<LevelFile> = levels.filter { it.place == place }.sortedBy { it.ordinal }

    fun place(place: Int): PlaceFile = read("places/p%02d.json".format(place))

    fun spec(level: LevelFile, difficulty: Difficulty): LevelSpec = level.spec(difficulty)

    fun species(id: String): Species? = speciesById[id]

    fun speciesName(id: String, portuguese: Boolean): String {
        val value = speciesById[id] ?: return id
        return if (portuguese) value.namePt else value.nameEn
    }

    fun nextLevel(id: String): LevelFile? {
        val index = levels.indexOfFirst { it.id == id }
        return levels.getOrNull(index + 1)
    }

    fun previousLevel(id: String): LevelFile? {
        val index = levels.indexOfFirst { it.id == id }
        return if (index > 0) levels[index - 1] else null
    }

    private inline fun <reified T> read(path: String): T =
        assets.open(path).bufferedReader().use { reader ->
            json.decodeFromString<T>(reader.readText())
        }
}
