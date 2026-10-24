package pt.tiago.ducknight.core.level

import kotlinx.serialization.Serializable
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.SpeciesCatalog
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank

/** The three difficulties. */
@Serializable
enum class Difficulty(val speedPercent: Int, val frameScalePercent: Int, val timeoutTicks: Int) {
    EASY(80, 125, 480),
    MEDIUM(100, 100, 390),
    HARD(120, 85, 300),
}

/** Level types. */
@Serializable
enum class LevelType {
    CLASSIC,
    DOUBLE,
    FLOCK,
    FILM,
    TIMED,
    PROTECT,
    CLAY,
    BOSS,
}

/** A species in a level's weighted pool. */
@Serializable
data class SpeciesWeight(val id: String, val weight: Int) {
    init {
        require(id.isNotBlank()) { "Species weight needs an id" }
        require(weight > 0) { "Species weight for $id must be positive" }
    }
}

/** Star thresholds, as scores. */
@Serializable
data class StarThresholds(val two: Int, val three: Int) {
    init {
        require(two > 0) { "Two-star threshold must be positive" }
        require(three > two) { "Three-star threshold must exceed two-star" }
    }
}

/** Weather and rules modifiers. */
@Serializable
enum class Modifier {
    WIND,
    FOG,
    SNOW,
    RAIN,
    NIGHT,
    COVER,
    DIVE,
    PROTECTED,
    GOLDEN,
    MANUAL_ADVANCE,
}

/**
 * Parameters that only some level types use. Keeping them in one nullable object preserves the
 * plain CLASSIC hash: a level without rules hashes exactly as it did before the other types existed.
 */
@Serializable
data class TypeRules(
    val flockSize: Int = 5,
    val flocks: Int = 3,
    val filmFrames: Int = 15,
    val timedTicks: Int = 3_600,
    val photoCooldownTicks: Int = 21,
    val protectedBirds: Int = 2,
    val clayPairs: Int = 10,
    val corksPerPair: Int = 3,
    val bossPhases: Int = 3,
    val bossPhotoLimit: Int = 15,
    val bossTimeTicks: Int = 5_400,
    val goalPages: Int = 3,
    val fogBanks: List<FogBank> = emptyList(),
    val coverBoxes: List<CoverBox> = emptyList(),
    val windStrength: Int = 12,
) {
    init {
        require(flockSize > 0 && flocks > 0) { "A flock needs a size and a count" }
        require(filmFrames > 0) { "A film level needs frames" }
        require(timedTicks > 0) { "A timed level needs ticks" }
        require(protectedBirds >= 0) { "Protected bird count cannot be negative" }
        require(clayPairs > 0 && corksPerPair > 0) { "A clay level needs pairs and corks" }
        require(bossPhases >= 3) { "A boss has three phases" }
        require(goalPages in 1..bossPhases) { "The album goal must fit the phases" }
        require(windStrength >= 0) { "Wind strength cannot be negative" }
    }
}

/** The accessibility settings that change the photo frame. */
@Serializable
data class GameOptions(
    val tolerancePercent: Int = 100,
    val biggerBirds: Boolean = false,
    val aimAssist: Boolean = false,
) {
    init {
        require(tolerancePercent in 85..125) { "Tolerance must be 85..125 %, was $tolerancePercent" }
    }
}

/**
 * One certified variant of a level (one level and one difficulty). The app, the simulated
 * photographer, the replay and the certificate verifier all consume this same object.
 */
@Serializable
data class LevelSpec(
    val id: String,
    val place: Int,
    val ordinal: Int,
    val type: LevelType,
    val difficulty: Difficulty,
    val seed: Long,
    val attemptIndex: Int = 0,
    val birds: Int,
    val photosPerBird: Int,
    val need: Int,
    val timeoutTicks: Int,
    val speedPercent: Int,
    val rampPercent: Int,
    val frameScalePercent: Int,
    val species: List<SpeciesWeight>,
    val patterns: List<FlightPattern> = emptyList(),
    val stars: StarThresholds,
    /** Weather and rule modifiers. Empty in plain CLASSIC, so its hash is unchanged. */
    val modifiers: Set<Modifier> = emptySet(),
    /** Parameters of the richer level types. Null for plain CLASSIC, so its hash is unchanged. */
    val rules: TypeRules? = null,
) {
    init {
        require(id.isNotBlank()) { "Level id must not be blank" }
        require(place in 1..12) { "Place must be 1..12" }
        require(ordinal > 0) { "Ordinal must be positive" }
        require(birds > 0) { "A level needs at least one bird" }
        require(photosPerBird > 0) { "A level needs at least one photo" }
        if (type == LevelType.BOSS) {
            require(need <= (rules?.goalPages ?: 3)) { "A boss need is an album size" }
        } else {
            require(need in 0..birds) { "Need $need must fit in $birds birds" }
        }
        require(timeoutTicks > 0) { "Timeout must be positive" }
        require(speedPercent in 50..200) { "Speed $speedPercent outside 50..200" }
        require(rampPercent >= 0) { "Ramp must not be negative" }
        require(frameScalePercent > 0) { "Frame scale must be positive" }
        require(species.isNotEmpty()) { "A level needs a species pool" }
        if (type == LevelType.DOUBLE) require(birds % 2 == 0) { "DOUBLE needs pairs" }
        if (type == LevelType.FLOCK) {
            val r = rules ?: TypeRules()
            require(birds == r.flocks * r.flockSize) { "FLOCK birds must fill its flocks" }
        }
        if (type == LevelType.CLAY) {
            val r = rules ?: TypeRules()
            require(r.clayPairs * 2 >= need) { "CLAY need must fit the plates" }
        }
    }

    /** The species patterns allowed in this level, or the union of the pool when unset. */
    fun allowedPatterns(): List<FlightPattern> =
        patterns.ifEmpty {
            species.map { SpeciesCatalog.byId(it.id) }
                .flatMap { it.patterns }
                .distinct()
        }

    companion object {
        /**
         * A CLASSIC level with the place-one species, used by the tests as the canonical variant.
         */
        fun classic(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            attempt: Int = 0,
            birds: Int = 10,
            need: Int = when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 6
                Difficulty.HARD -> 7
            },
            id: String = "test-classic",
            place: Int = 1,
            ordinal: Int = 1,
            stars: StarThresholds = StarThresholds(two = 4_000, three = 6_000),
        ): LevelSpec = LevelSpec(
            id = id,
            place = place,
            ordinal = ordinal,
            type = LevelType.CLASSIC,
            difficulty = difficulty,
            seed = seed,
            attemptIndex = attempt,
            birds = birds,
            photosPerBird = 3,
            need = need,
            timeoutTicks = difficulty.timeoutTicks,
            speedPercent = difficulty.speedPercent,
            rampPercent = 2,
            frameScalePercent = difficulty.frameScalePercent,
            species = listOf(
                SpeciesWeight("mallard", 50),
                SpeciesWeight("teal", 35),
                SpeciesWeight("gadwall", 15),
            ),
            stars = stars,
        )

        private fun placeOneSpecies(): List<SpeciesWeight> = listOf(
            SpeciesWeight("mallard", 50),
            SpeciesWeight("teal", 35),
            SpeciesWeight("gadwall", 15),
        )

        /** Ten birds in five pairs, three photos for each pair. */
        fun double(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            birds: Int = 10,
            need: Int = 6,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-double", place = 2, ordinal = 51, type = LevelType.DOUBLE,
            difficulty = difficulty, seed = seed, birds = birds, photosPerBird = 3, need = need,
            timeoutTicks = difficulty.timeoutTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 2, frameScalePercent = difficulty.frameScalePercent,
            species = placeOneSpecies(), stars = stars, modifiers = modifiers, rules = rules,
        )

        /** Three to five V flocks that cross the sky once, six photos per flock. */
        fun flock(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            flocks: Int = 3,
            flockSize: Int = 5,
            need: Int = 8,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(flocks = flocks, flockSize = flockSize),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-flock", place = 3, ordinal = 101, type = LevelType.FLOCK,
            difficulty = difficulty, seed = seed, birds = flocks * flockSize, photosPerBird = 6,
            need = need, timeoutTicks = difficulty.timeoutTicks,
            speedPercent = difficulty.speedPercent, rampPercent = 2,
            frameScalePercent = difficulty.frameScalePercent, species = placeOneSpecies(),
            patterns = listOf(FlightPattern.FORMATION, FlightPattern.GLIDE),
            stars = stars, modifiers = modifiers, rules = rules,
        )

        /** A shared roll: N frames for M birds. */
        fun film(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            birds: Int = 10,
            filmFrames: Int = 15,
            need: Int = 6,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(filmFrames = filmFrames),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-film", place = 4, ordinal = 151, type = LevelType.FILM,
            difficulty = difficulty, seed = seed, birds = birds, photosPerBird = 3, need = need,
            timeoutTicks = difficulty.timeoutTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 2, frameScalePercent = difficulty.frameScalePercent,
            species = placeOneSpecies(), stars = stars, modifiers = modifiers, rules = rules,
        )

        /** A fixed number of seconds of continuous birds, with a 0.35 s cooldown. */
        fun timed(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            seconds: Int = 60,
            need: Int = 8,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(timedTicks = seconds * 60),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-timed", place = 4, ordinal = 152, type = LevelType.TIMED,
            difficulty = difficulty, seed = seed, birds = 100, photosPerBird = 999, need = need,
            timeoutTicks = difficulty.timeoutTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 2, frameScalePercent = difficulty.frameScalePercent,
            species = placeOneSpecies(), stars = stars, modifiers = modifiers, rules = rules,
        )

        /** Classic or double with protected birds crossing high above the targets. */
        fun protect(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            birds: Int = 8,
            protectedBirds: Int = 2,
            need: Int = 5,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(protectedBirds = protectedBirds),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-protect", place = 5, ordinal = 201, type = LevelType.PROTECT,
            difficulty = difficulty, seed = seed, birds = birds, photosPerBird = 3, need = need,
            timeoutTicks = difficulty.timeoutTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 2, frameScalePercent = difficulty.frameScalePercent,
            species = placeOneSpecies(), stars = stars, modifiers = modifiers, rules = rules,
        )

        /** Clay plates at the fair: pairs, three corks per pair. */
        fun clay(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            pairs: Int = 10,
            need: Int = 10,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(clayPairs = pairs),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-clay", place = 2, ordinal = 55, type = LevelType.CLAY,
            difficulty = difficulty, seed = seed, birds = pairs * 2, photosPerBird = 3, need = need,
            timeoutTicks = difficulty.timeoutTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 0, frameScalePercent = difficulty.frameScalePercent,
            species = placeOneSpecies(), stars = stars, modifiers = modifiers, rules = rules,
        )

        /** The legendary bird: three good photos fill the album. */
        fun boss(
            seed: Long,
            difficulty: Difficulty = Difficulty.MEDIUM,
            pages: Int = 3,
            modifiers: Set<Modifier> = emptySet(),
            rules: TypeRules = TypeRules(goalPages = pages),
            stars: StarThresholds = StarThresholds(4_000, 6_000),
        ): LevelSpec = LevelSpec(
            id = "test-boss", place = 12, ordinal = 600, type = LevelType.BOSS,
            difficulty = difficulty, seed = seed, birds = 1, photosPerBird = 15, need = pages,
            timeoutTicks = rules.bossTimeTicks, speedPercent = difficulty.speedPercent,
            rampPercent = 0, frameScalePercent = difficulty.frameScalePercent,
            species = listOf(SpeciesWeight("great_duck", 100)),
            patterns = listOf(FlightPattern.BOSS), stars = stars,
            modifiers = modifiers, rules = rules,
        )
    }
}
