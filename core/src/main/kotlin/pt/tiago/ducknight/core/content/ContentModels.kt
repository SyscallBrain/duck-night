package pt.tiago.ducknight.core.content

import kotlinx.serialization.Serializable
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.replay.InputLog
import pt.tiago.ducknight.core.species.FlightPattern

@Serializable
data class VariantFile(
    val levelSeed: Long,
    val birds: Int,
    val photosPerBird: Int,
    val need: Int,
    val timeoutTicks: Int,
    val speed: Int,
    val ramp: Int,
    val frameScale: Int,
    val species: List<SpeciesWeight>,
    val patterns: List<FlightPattern> = emptyList(),
    val modifiers: Set<Modifier> = emptySet(),
    val rules: TypeRules? = null,
    val stars: StarThresholds,
)

@Serializable
data class LevelFile(
    val id: String,
    val place: Int,
    val ordinal: Int,
    val type: LevelType,
    val family: String,
    val tags: List<String> = emptyList(),
    val tutorial: String? = null,
    val variants: Map<String, VariantFile>,
) {
    fun variant(difficulty: Difficulty): VariantFile =
        variants[difficulty.name] ?: error("Level $id has no ${difficulty.name} variant")

    fun spec(difficulty: Difficulty): LevelSpec {
        val v = variant(difficulty)
        return LevelSpec(
            id = id,
            place = place,
            ordinal = ordinal,
            type = type,
            difficulty = difficulty,
            seed = v.levelSeed,
            attemptIndex = 0,
            birds = v.birds,
            photosPerBird = v.photosPerBird,
            need = v.need,
            timeoutTicks = v.timeoutTicks,
            speedPercent = v.speed,
            rampPercent = v.ramp,
            frameScalePercent = v.frameScale,
            species = v.species,
            patterns = v.patterns,
            stars = v.stars,
            modifiers = v.modifiers,
            rules = v.rules,
        )
    }

    fun withSpec(spec: LevelSpec): LevelFile {
        val v = variant(spec.difficulty)
        return copy(
            variants = variants + (spec.difficulty.name to v.copy(
                birds = spec.birds,
                photosPerBird = spec.photosPerBird,
                need = spec.need,
                timeoutTicks = spec.timeoutTicks,
                speed = spec.speedPercent,
                ramp = spec.rampPercent,
                frameScale = spec.frameScalePercent,
                species = spec.species,
                patterns = spec.patterns,
                modifiers = spec.modifiers,
                rules = spec.rules,
                stars = spec.stars,
            )),
        )
    }
}

@Serializable
data class PlaceFile(
    val id: Int,
    val nameKey: String,
    val scene: String,
    val weather: String,
    val music: String,
    val bossLevel: String? = null,
    val introCards: List<String> = emptyList(),
)

@Serializable
data class Catalog(
    val simVersion: Int,
    val botVersion: Int,
    val contentVersion: Int,
    val places: List<Int>,
    val levelIds: List<String>,
    val files: Map<String, String>,
)

@Serializable
data class CalibrationFile(
    val levelId: String,
    val difficulty: String,
    val profileId: String,
    val runs: Int,
    val successes: Int,
    val successRate: Double,
    val meanScore: Double,
    val meanPhotos: Double,
    val p35: Int,
    val p70: Int,
)

@Serializable
data class HumanSample(
    val profile: String,
    val runs: Int,
    val successes: Int,
    val successRate: Double,
    val meanScore: Double,
    val p35: Int,
    val p70: Int,
)

@Serializable
data class WitnessFile(
    val attemptIndex: Int,
    val seed: Long,
    val finalTick: Int,
    val finalHash: Long,
    val inputLog: InputLog,
)

@Serializable
data class Certificate(
    val levelId: String,
    val difficulty: String,
    val variantHash: String,
    val simVersion: Int,
    val botVersion: Int,
    val human: HumanSample,
    val stars: StarThresholds,
    val witness: WitnessFile,
)

@Serializable
data class DailyRound(
    val levelId: String,
    val difficulty: String,
)

@Serializable
data class DailyEntry(
    val rounds: List<DailyRound>,
)

@Serializable
data class DailyPool(
    val version: Int,
    val entries: List<DailyEntry>,
)

@Serializable
data class FamilyTemplate(
    val id: String,
    val type: LevelType,
    val tags: List<String> = emptyList(),
    val patterns: List<FlightPattern> = emptyList(),
    val minBirds: Int = 10,
    val maxBirds: Int = 10,
)

@Serializable
data class FamilyCatalog(val families: List<FamilyTemplate>)

@Serializable
data class ArcadeConfig(
    val games: List<String> = listOf("A", "B", "C"),
    val roundSpeedRampPercent: Int = 7,
    val minCorrect: String = "min(10, 5 + round)",
)

@Serializable
data class FairConfig(
    val clayPairsPerRound: Int = 10,
    val corksPerPair: Int = 3,
    val gallerySeconds: Int = 60,
    val galleryRacks: Int = 3,
)

@Serializable
data class AssetManifest(
    val simVersion: Int,
    val botVersion: Int,
    val contentVersion: Int,
    val files: Map<String, String>,
)
