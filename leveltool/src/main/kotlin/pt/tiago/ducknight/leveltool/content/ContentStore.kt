package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.BOT_VERSION
import pt.tiago.ducknight.core.CONTENT_VERSION
import pt.tiago.ducknight.core.SIM_VERSION
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.species.SpeciesCatalog
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class ContentStore(val root: Path) {
    val catalogPath: Path = root.resolve("catalog.json")
    val speciesPath: Path = root.resolve("species.json")
    val placesDir: Path = root.resolve("places")
    val levelsDir: Path = root.resolve("levels")
    val certificatesDir: Path = root.resolve("certificates")
    val calibrationDir: Path = root.resolve("calibration")
    val dailyDir: Path = root.resolve("daily")
    val arcadeDir: Path = root.resolve("arcade")
    val fairDir: Path = root.resolve("fair")
    val templatesDir: Path = root.resolve("templates")
    val candidatesDir: Path = root.resolve("candidates")

    fun ensureScaffold(place: Int = 1) {
        Files.createDirectories(placesDir)
        Files.createDirectories(levelsDir)
        Files.createDirectories(certificatesDir)
        Files.createDirectories(calibrationDir)
        Files.createDirectories(dailyDir)
        Files.createDirectories(arcadeDir)
        Files.createDirectories(fairDir)
        Files.createDirectories(templatesDir)
        JsonFiles.writeValue(speciesPath, SpeciesCatalog.all)
        val placePath = placesDir.resolve("p%02d.json".format(place))
        val meta = placeMetadata[place] ?: PlaceMetadata("village_pond", "clear", "guitar")
        JsonFiles.writeValue(
            placePath,
            PlaceFile(
                id = place,
                nameKey = "place_${place}_name",
                scene = meta.scene,
                weather = meta.weather,
                music = meta.music,
                bossLevel = "p%02dl050".format(place),
                introCards = emptyList(),
            ),
        )
        if (!templatesDir.resolve("families.json").exists()) {
            JsonFiles.writeValue(templatesDir.resolve("families.json"), defaultFamilies())
        }
        if (!dailyDir.resolve("pool.json").exists()) {
            JsonFiles.writeValue(dailyDir.resolve("pool.json"), DailyPool(version = 1, entries = emptyList()))
        }
        if (!arcadeDir.resolve("config.json").exists()) {
            JsonFiles.writeValue(arcadeDir.resolve("config.json"), ArcadeConfig())
        }
        if (!fairDir.resolve("config.json").exists()) {
            JsonFiles.writeValue(fairDir.resolve("config.json"), FairConfig())
        }
        if (!catalogPath.exists()) {
            JsonFiles.writeValue(
                catalogPath,
                Catalog(
                    simVersion = SIM_VERSION,
                    botVersion = BOT_VERSION,
                    contentVersion = CONTENT_VERSION,
                    places = listOf(place),
                    levelIds = emptyList(),
                    files = emptyMap(),
                ),
            )
        }
    }

    fun loadCatalog(): Catalog = JsonFiles.readValue(catalogPath)

    fun loadLevel(id: String): LevelFile {
        val place = id.substring(1, 3).toInt()
        val path = levelPath(place, id)
        return JsonFiles.readValue(path)
    }

    fun writeLevel(level: LevelFile) {
        val path = levelPath(level.place, level.id)
        JsonFiles.writeValue(path, level)
    }

    fun levelPath(place: Int, id: String): Path = levelsDir.resolve("p%02d".format(place)).resolve("$id.json")

    fun levelFiles(): List<LevelFile> {
        if (!levelsDir.exists()) return emptyList()
        return Files.walk(levelsDir).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".json") }
                .map { JsonFiles.readValue<LevelFile>(it) }
                .sorted(compareBy { it.id })
                .toList()
        }
    }

    fun certificatePath(levelId: String, difficulty: Difficulty): Path =
        certificatesDir.resolve("$levelId-${difficultyPrefix(difficulty)}.json")

    fun calibrationPath(levelId: String, difficulty: Difficulty): Path =
        calibrationDir.resolve("$levelId-${difficultyPrefix(difficulty)}.json")

    fun writeCertificate(certificate: Certificate) {
        val difficulty = Difficulty.valueOf(certificate.difficulty)
        JsonFiles.writeValue(certificatePath(certificate.levelId, difficulty), certificate)
    }

    fun loadCertificates(): List<Certificate> {
        if (!certificatesDir.exists()) return emptyList()
        return Files.walk(certificatesDir).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".json") }
                .map { JsonFiles.readValue<Certificate>(it) }
                .sorted(compareBy({ it.levelId }, { it.difficulty }))
                .toList()
        }
    }

    fun loadCalibration(levelId: String, difficulty: Difficulty): CalibrationFile? {
        val path = calibrationPath(levelId, difficulty)
        return if (path.exists()) JsonFiles.readValue(path) else null
    }

    fun publishedFiles(): List<Path> {
        val files = mutableListOf<Path>()
        if (speciesPath.exists()) files.add(speciesPath)
        for (dir in listOf(placesDir, levelsDir, certificatesDir, dailyDir, arcadeDir, fairDir, templatesDir)) {
            if (!dir.exists()) continue
            Files.walk(dir).use { stream ->
                stream.filter { it.isRegularFile() && it.name.endsWith(".json") }
                    .forEach { files.add(it) }
            }
        }
        return files.distinct().sortedBy { root.relativize(it).toString() }
    }

    fun updateCatalog() {
        val levels = levelFiles()
        val places = Files.list(placesDir).use { stream ->
            stream.filter { it.isRegularFile() && it.name.matches(Regex("p\\d{2}\\.json")) }
                .map { it.name.removeSuffix(".json").removePrefix("p").toInt() }
                .sorted()
                .toList()
        }
        val files = publishedFiles().associate { path ->
            root.relativize(path).toString().replace('\\', '/') to JsonFiles.sha256(path)
        }.toSortedMap()
        val catalog = Catalog(
            simVersion = SIM_VERSION,
            botVersion = BOT_VERSION,
            contentVersion = CONTENT_VERSION,
            places = places,
            levelIds = levels.map { it.id },
            files = files,
        )
        JsonFiles.writeValue(catalogPath, catalog)
    }

    fun relative(path: Path): String = root.relativize(path).toString().replace('\\', '/')

    fun difficultyPrefix(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EASY -> "E"
        Difficulty.MEDIUM -> "M"
        Difficulty.HARD -> "H"
    }

    private fun defaultFamilies(): FamilyCatalog = FamilyCatalog(
        families = buildList {
            add(FamilyTemplate("classic-open", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("basic")))
            add(FamilyTemplate("classic-glide", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("glide")))
            add(FamilyTemplate("classic-fast", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("fast")))
            add(FamilyTemplate("double-pair", pt.tiago.ducknight.core.level.LevelType.DOUBLE))
            add(FamilyTemplate("double-mixed", pt.tiago.ducknight.core.level.LevelType.DOUBLE))
            add(FamilyTemplate("flock-v", pt.tiago.ducknight.core.level.LevelType.FLOCK))
            add(FamilyTemplate("flock-mixed", pt.tiago.ducknight.core.level.LevelType.FLOCK))
            add(FamilyTemplate("film-short", pt.tiago.ducknight.core.level.LevelType.FILM))
            add(FamilyTemplate("film-tight", pt.tiago.ducknight.core.level.LevelType.FILM))
            add(FamilyTemplate("timed-stream", pt.tiago.ducknight.core.level.LevelType.TIMED))
            add(FamilyTemplate("timed-waves", pt.tiago.ducknight.core.level.LevelType.TIMED))
            add(FamilyTemplate("protect-cross", pt.tiago.ducknight.core.level.LevelType.PROTECT))
            add(FamilyTemplate("protect-mixed", pt.tiago.ducknight.core.level.LevelType.PROTECT))
            add(FamilyTemplate("clay-pairs", pt.tiago.ducknight.core.level.LevelType.CLAY))
            add(FamilyTemplate("clay-cross", pt.tiago.ducknight.core.level.LevelType.CLAY))
            add(FamilyTemplate("meadow-protect", pt.tiago.ducknight.core.level.LevelType.PROTECT, listOf("place5")))
            add(FamilyTemplate("snow-advance", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place6")))
            add(FamilyTemplate("salt-dive", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place7")))
            add(FamilyTemplate("autumn-hop", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place8")))
            add(FamilyTemplate("night-sweep", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place9")))
            add(FamilyTemplate("storm-front", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place10")))
            add(FamilyTemplate("aurora-run", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place11")))
            add(FamilyTemplate("great-finale", pt.tiago.ducknight.core.level.LevelType.CLASSIC, listOf("place12")))
            for (boss in 1..12) add(FamilyTemplate("boss-p%02d".format(boss), pt.tiago.ducknight.core.level.LevelType.BOSS))
        },
    )

    private data class PlaceMetadata(val scene: String, val weather: String, val music: String)

    private val placeMetadata = mapOf(
        1 to PlaceMetadata("village_pond", "clear", "guitar"),
        2 to PlaceMetadata("wheat_field", "golden", "accordion"),
        3 to PlaceMetadata("windy_pinewood", "windy", "flute"),
        4 to PlaceMetadata("misty_marsh", "fog", "drone"),
        5 to PlaceMetadata("stork_meadow", "pink", "marimba"),
        6 to PlaceMetadata("snowy_ridge", "snow", "icy_pads"),
        7 to PlaceMetadata("salt_pans", "reflections", "water_percussion"),
        8 to PlaceMetadata("autumn_wood", "leaves", "bassoon"),
        9 to PlaceMetadata("heath_night", "night", "dark_drone"),
        10 to PlaceMetadata("storm_delta", "storm", "percussion"),
        11 to PlaceMetadata("aurora_fjord", "aurora", "choir"),
        12 to PlaceMetadata("great_nest", "moon", "full_theme"),
    )

    fun readSpecies(): List<pt.tiago.ducknight.core.species.Species> =
        JsonFiles.readValue(speciesPath)

    fun writeDailyPool(pool: DailyPool) {
        JsonFiles.writeValue(dailyDir.resolve("pool.json"), pool)
    }

    fun readDailyPool(): DailyPool = JsonFiles.readValue(dailyDir.resolve("pool.json"))

    fun writeText(relative: String, text: String) {
        val path = root.resolve(relative)
        Files.createDirectories(path.parent)
        Files.writeString(path, text, StandardCharsets.UTF_8)
    }
}
