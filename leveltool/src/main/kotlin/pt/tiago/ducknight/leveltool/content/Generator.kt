package pt.tiago.ducknight.leveltool.content

import pt.tiago.ducknight.core.content.*
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.random.SplitMix64
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.SpeciesCatalog
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank
import java.nio.file.Files

/**
 * Generates the 600 campaign levels. Places 1-4 share the first families; places 5-12 add their
 * own signature family and use the exact type mix of each place.
 */
object Generator {
    const val LEVELS_PER_PLACE = 50

    private val bossByPlace = mapOf(
        1 to "old_mallard",
        2 to "twin_teal",
        3 to "wind_goose",
        4 to "mist_goose",
        5 to "thief_crow",
        6 to "royal_snow_goose",
        7 to "royal_grebe",
        8 to "imperial_mandarin",
        9 to "ghost_duck",
        10 to "thunder_flock",
        11 to "golden_eider",
        12 to "great_duck",
    )

    private val placeModifiers = mapOf(
        3 to listOf(Modifier.WIND),
        4 to listOf(Modifier.FOG),
        5 to listOf(Modifier.PROTECTED),
        6 to listOf(Modifier.SNOW, Modifier.MANUAL_ADVANCE),
        7 to listOf(Modifier.DIVE),
        8 to listOf(Modifier.COVER),
        9 to listOf(Modifier.NIGHT),
        10 to listOf(Modifier.RAIN),
        11 to listOf(Modifier.GOLDEN),
    )

    private val introTypes = mapOf(
        2 to mapOf(1 to LevelType.DOUBLE, 3 to LevelType.CLAY, 6 to LevelType.DOUBLE),
        3 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.FLOCK),
        4 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.DOUBLE, 6 to LevelType.TIMED),
        5 to mapOf(1 to LevelType.PROTECT, 3 to LevelType.PROTECT, 6 to LevelType.PROTECT),
        6 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
        7 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
        8 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
        9 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.DOUBLE, 6 to LevelType.DOUBLE),
        10 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
        11 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
        12 to mapOf(1 to LevelType.CLASSIC, 3 to LevelType.FLOCK, 6 to LevelType.DOUBLE),
    )

    private val signatureFamilies = mapOf(
        5 to "meadow-protect",
        6 to "snow-advance",
        7 to "salt-dive",
        8 to "autumn-hop",
        9 to "night-sweep",
        10 to "storm-front",
        11 to "aurora-run",
        12 to "great-finale",
    )

    private val regularById = SpeciesCatalog.all
        .filter { !it.protected && it.id !in bossByPlace.values }
        .associateBy { it.id }

    /** The exact type mix of each place. */
    fun mixFor(place: Int): Map<LevelType, Int> = when (place) {
        1 -> mapOf(LevelType.CLASSIC to 42, LevelType.FILM to 7, LevelType.BOSS to 1)
        2 -> mapOf(
            LevelType.CLASSIC to 16, LevelType.DOUBLE to 22, LevelType.FILM to 5,
            LevelType.CLAY to 6, LevelType.BOSS to 1,
        )
        3 -> mapOf(
            LevelType.CLASSIC to 10, LevelType.DOUBLE to 10, LevelType.FLOCK to 18,
            LevelType.FILM to 6, LevelType.CLAY to 5, LevelType.BOSS to 1,
        )
        4 -> mapOf(
            LevelType.CLASSIC to 12, LevelType.DOUBLE to 12, LevelType.FLOCK to 8,
            LevelType.FILM to 5, LevelType.TIMED to 8, LevelType.CLAY to 4,
            LevelType.BOSS to 1,
        )
        5 -> mapOf(
            LevelType.CLASSIC to 8, LevelType.DOUBLE to 8, LevelType.FLOCK to 6,
            LevelType.FILM to 4, LevelType.TIMED to 4, LevelType.PROTECT to 15,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
        12 -> mapOf(
            LevelType.CLASSIC to 8, LevelType.DOUBLE to 9, LevelType.FLOCK to 8,
            LevelType.FILM to 6, LevelType.TIMED to 6, LevelType.PROTECT to 8,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
        in 6..11 -> mapOf(
            LevelType.CLASSIC to 9, LevelType.DOUBLE to 9, LevelType.FLOCK to 7,
            LevelType.FILM to 6, LevelType.TIMED to 5, LevelType.PROTECT to 9,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
        else -> error("Place must be 1..12")
    }

    /** Deterministic type sequence for every place; the last level is always the boss. */
    fun typesFor(place: Int): List<LevelType> {
        require(place in 1..12) { "Place must be 1..12" }
        if (place == 1) {
            val film = setOf(7, 14, 21, 28, 35, 42, 48)
            return buildList {
                for (ordinal in 1..49) add(if (ordinal in film) LevelType.FILM else LevelType.CLASSIC)
                add(LevelType.BOSS)
            }
        }
        val remaining = mixFor(place).filterKeys { it != LevelType.BOSS }.toMutableMap()
        val result = mutableListOf<LevelType>()
        for (ordinal in 1..49) {
            val forced = introTypes[place]?.get(ordinal)?.takeIf { (remaining[it] ?: 0) > 0 }
            val choice = if (forced != null && allowed(result, forced)) {
                forced
            } else {
                chooseType(place, ordinal, remaining, result)
            } ?: error("place $place cannot fill ordinal $ordinal: $remaining")
            result += choice
            remaining[choice] = remaining.getValue(choice) - 1
        }
        check(remaining.values.all { it == 0 }) { "place $place left $remaining" }
        result += LevelType.BOSS
        return result
    }

    private fun chooseType(
        place: Int,
        ordinal: Int,
        remaining: Map<LevelType, Int>,
        result: List<LevelType>,
    ): LevelType? {
        val nextForced = introTypes[place]?.get(ordinal + 1)
        val avoid = if (nextForced != null && result.lastOrNull() == nextForced) nextForced else null
        val viable = remaining.keys.filter {
            (remaining[it] ?: 0) > 0 && allowed(result, it) && it != avoid
        }
        return (viable.ifEmpty { remaining.keys.filter { (remaining[it] ?: 0) > 0 } })
            .sortedWith(
                compareByDescending<LevelType> { remaining[it] ?: 0 }
                    .thenBy { (it.ordinal + place + ordinal) % LevelType.entries.size },
            )
            .firstOrNull()
    }

    private fun allowed(result: List<LevelType>, type: LevelType): Boolean {
        if (type == LevelType.CLAY && result.lastOrNull() == LevelType.CLAY) return false
        val n = result.size
        if (n >= 2 && result[n - 1] == type && result[n - 2] == type) return false
        return true
    }

    fun generate(
        store: ContentStore,
        place: Int = 1,
        from: Int = 1,
        to: Int = 50,
    ): List<LevelFile> {
        require(place in 1..12)
        require(from in 1..50 && to in 1..50 && from <= to)
        store.ensureScaffold(place)
        Files.createDirectories(store.candidatesDir.resolve("p%02d".format(place)))
        val types = typesFor(place)
        val levels = mutableListOf<LevelFile>()
        for (ordinal in from..to) {
            val id = "p%02dl%03d".format(place, ordinal)
            val type = types[ordinal - 1]
            val level = LevelFile(
                id = id,
                place = place,
                ordinal = ordinal,
                type = type,
                family = familyFor(place, ordinal, type),
                tags = buildList {
                    add("place$place")
                    if (place == 1 && ordinal <= 10) add("tutorial")
                },
                tutorial = if (place == 1 && ordinal <= 10) "tutorial_$ordinal" else null,
                variants = Difficulty.entries.associate { difficulty ->
                    difficulty.name to variant(place, ordinal, difficulty, type)
                },
            )
            for (difficulty in Difficulty.entries) level.spec(difficulty)
            val candidate = store.candidatesDir.resolve("p%02d".format(place)).resolve("$id.json")
            JsonFiles.writeValue(candidate, level)
            store.writeLevel(level)
            levels += level
        }
        store.updateCatalog()
        return levels
    }

    private fun familyFor(place: Int, ordinal: Int, type: LevelType): String {
        if (type == LevelType.BOSS) return "boss-p%02d".format(place)
        if (ordinal == 1) signatureFamilies[place]?.let { return it }
        return when (type) {
            LevelType.CLASSIC -> listOf("classic-open", "classic-glide", "classic-fast")[(ordinal - 1) % 3]
            LevelType.DOUBLE -> if (ordinal % 2 == 0) "double-pair" else "double-mixed"
            LevelType.FLOCK -> if (ordinal % 2 == 0) "flock-v" else "flock-mixed"
            LevelType.FILM -> if (ordinal % 2 == 0) "film-tight" else "film-short"
            LevelType.TIMED -> if (ordinal % 2 == 0) "timed-stream" else "timed-waves"
            LevelType.PROTECT -> if (ordinal % 2 == 0) "protect-cross" else "protect-mixed"
            LevelType.CLAY -> if (ordinal % 2 == 0) "clay-pairs" else "clay-cross"
            LevelType.BOSS -> error("handled above")
        }
    }

    private fun variant(place: Int, ordinal: Int, difficulty: Difficulty, type: LevelType): VariantFile {
        val seed = SplitMix64.mix(place * 100_000L + ordinal * 31L + difficulty.ordinal)
        val modifiers = modifiersFor(place, ordinal, type)
        val (birds, photos, need, baseRules, patterns) = rulesFor(place, ordinal, difficulty, type)
        val rules = rulesWithModifiers(baseRules, type, modifiers, difficulty)
        return VariantFile(
            levelSeed = seed,
            birds = birds,
            photosPerBird = photos,
            need = need,
            timeoutTicks = if (type == LevelType.BOSS) 5_400 else difficulty.timeoutTicks,
            speed = if (type == LevelType.BOSS) 50 else difficulty.speedPercent,
            ramp = when (difficulty) {
                Difficulty.EASY -> 1
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 5
            },
            frameScale = if (type == LevelType.BOSS) 125 else difficulty.frameScalePercent,
            species = speciesFor(place, type, modifiers),
            patterns = patterns,
            modifiers = modifiers,
            rules = rules,
            stars = StarThresholds(two = 4_000, three = 6_000),
        )
    }

    private fun rulesWithModifiers(
        base: TypeRules?,
        type: LevelType,
        modifiers: Set<Modifier>,
        difficulty: Difficulty,
    ): TypeRules? {
        if (base == null && modifiers.isEmpty() && type != LevelType.PROTECT) return null
        var rules = base ?: TypeRules()
        if (Modifier.WIND in modifiers) rules = rules.copy(windStrength = 16)
        if (Modifier.FOG in modifiers) rules = rules.copy(fogBanks = listOf(FogBank(60, 40), FogBank(150, 50)))
        if (Modifier.COVER in modifiers) {
            rules = rules.copy(coverBoxes = listOf(CoverBox(20, 190, 42, 44), CoverBox(178, 196, 44, 38)))
        }
        if (type == LevelType.PROTECT || Modifier.PROTECTED in modifiers) {
            rules = rules.copy(protectedBirds = protectedCount(difficulty))
        }
        return rules
    }

    private fun rulesFor(
        place: Int,
        ordinal: Int,
        difficulty: Difficulty,
        type: LevelType,
    ): RulesTuple {
        val need = needFor(place, ordinal, difficulty)
        return when (type) {
            LevelType.CLASSIC, LevelType.DOUBLE ->
                RulesTuple(10, 3, need, null, emptyList())
            LevelType.PROTECT ->
                RulesTuple(10, 3, need, TypeRules(protectedBirds = protectedCount(difficulty)), emptyList())
            LevelType.FLOCK ->
                RulesTuple(15, 6, need + 1, TypeRules(flocks = 3, flockSize = 5), listOf(FlightPattern.FORMATION, FlightPattern.GLIDE))
            LevelType.FILM ->
                RulesTuple(
                    10, 3, need,
                    TypeRules(
                        filmFrames = when (difficulty) {
                            Difficulty.EASY -> 15
                            Difficulty.MEDIUM -> 14
                            Difficulty.HARD -> 13
                        },
                    ),
                    emptyList(),
                )
            LevelType.TIMED ->
                RulesTuple(100, 999, if (place <= 4) 7 else 8, TypeRules(timedTicks = 3_600, photoCooldownTicks = 21), emptyList())
            LevelType.CLAY ->
                RulesTuple(20, 3, 10, TypeRules(clayPairs = 10, corksPerPair = 3), emptyList())
            LevelType.BOSS ->
                RulesTuple(
                    1, 15, 3,
                    TypeRules(goalPages = 3, bossPhases = 3, bossPhotoLimit = 15, bossTimeTicks = 5_400),
                    listOf(FlightPattern.BOSS),
                )
        }
    }

    private data class RulesTuple(
        val birds: Int,
        val photos: Int,
        val need: Int,
        val rules: TypeRules?,
        val patterns: List<FlightPattern>,
    )

    private fun needFor(place: Int, ordinal: Int, difficulty: Difficulty): Int {
        val base = when (difficulty) {
            Difficulty.EASY -> if (place <= 4) 5 else 6
            Difficulty.MEDIUM -> if (place <= 4) 6 else 7
            Difficulty.HARD -> if (place <= 4) 7 else 8
        }
        val late = if (ordinal in 45..49) 1 else 0
        return (base - late).coerceAtLeast(1)
    }

    private fun protectedCount(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 1
        Difficulty.MEDIUM -> 2
        Difficulty.HARD -> 3
    }

    private fun modifiersFor(place: Int, ordinal: Int, type: LevelType): Set<Modifier> {
        if (type == LevelType.CLAY || type == LevelType.BOSS) return emptySet()
        val own = placeModifiers[place].orEmpty()
        val intro = ordinal == 1 || ordinal == 3 || ordinal == 6
        val max = if (place >= 10) 3 else 2
        val mods = linkedSetOf<Modifier>()
        for (modifier in own) {
            if (type == LevelType.PROTECT && modifier == Modifier.PROTECTED) continue
            if (modifier == Modifier.MANUAL_ADVANCE && type == LevelType.TIMED) continue
            if (intro || roll(place, ordinal, modifier) < ownChance(modifier)) mods += modifier
        }
        if (intro && mods.isEmpty()) {
            val candidates = introducedModifiers(place).filter {
                it != Modifier.MANUAL_ADVANCE && it != Modifier.NIGHT && it != Modifier.FOG
            }.ifEmpty { introducedModifiers(place) }
            if (candidates.isNotEmpty()) {
                val take = if (place == 12) 2 else 1
                val start = (ordinal / 3) % candidates.size
                val rotated = candidates.drop(start) + candidates.take(start)
                for (modifier in rotated) {
                    if (mods.size >= take) break
                    if (modifier in mods) continue
                    mods += modifier
                }
            }
        }
        if (mods.size < max) {
            for (modifier in introducedModifiers(place)) {
                if (mods.size >= max) break
                if (modifier in mods) continue
                if (modifier == Modifier.MANUAL_ADVANCE && type == LevelType.TIMED) continue
                if (modifier == Modifier.NIGHT && Modifier.FOG in mods) continue
                if (modifier == Modifier.FOG && Modifier.NIGHT in mods) continue
                if (roll(place, ordinal, modifier) < 20) mods += modifier
            }
        }
        if (Modifier.NIGHT in mods) mods.remove(Modifier.FOG)
        // A small flashlight and a shared flock roll are not playable together: the lamp hides
        // most of the flock and the group shot cannot be aimed. Keep NIGHT for solo/double/timed.
        if (Modifier.NIGHT in mods && type in setOf(LevelType.FLOCK, LevelType.FILM)) {
            mods.remove(Modifier.NIGHT)
        }
        return mods
    }

    private fun ownChance(modifier: Modifier): Int = when (modifier) {
        Modifier.MANUAL_ADVANCE -> 40
        else -> 60
    }

    private fun introducedModifiers(place: Int): List<Modifier> =
        (3 until place).flatMap { placeModifiers[it].orEmpty() }.distinct()

    private fun roll(place: Int, ordinal: Int, modifier: Modifier): Int {
        val mixed = SplitMix64.mix(place * 100_000L + ordinal * 31L + modifier.ordinal)
        return ((mixed ushr 1) % 100).toInt()
    }

    private fun speciesFor(place: Int, type: LevelType, modifiers: Set<Modifier>): List<SpeciesWeight> {
        if (type == LevelType.BOSS) {
            return listOf(SpeciesWeight(bossByPlace.getValue(place), 100))
        }
        val targets = targetPool(place).toMutableList()
        if (type == LevelType.PROTECT || Modifier.PROTECTED in modifiers) {
            targets += protectedPool(place)
        }
        return targets
    }

    private fun targetPool(place: Int): List<SpeciesWeight> = when (place) {
        1 -> listOf(
            SpeciesWeight("mallard", 45), SpeciesWeight("teal", 30),
            SpeciesWeight("gadwall", 12), SpeciesWeight("wigeon", 10),
            SpeciesWeight("golden", 3),
        )
        2 -> listOf(SpeciesWeight("shoveler", 50), SpeciesWeight("pintail", 30), SpeciesWeight("garganey", 20))
        3 -> listOf(
            SpeciesWeight("greylag", 40), SpeciesWeight("barnacle", 30),
            SpeciesWeight("ruddy_shelduck", 20), SpeciesWeight("mallard", 10),
        )
        4 -> listOf(
            SpeciesWeight("shelduck", 35), SpeciesWeight("coot", 30),
            SpeciesWeight("pochard", 25), SpeciesWeight("teal", 10),
        )
        5 -> listOf(
            SpeciesWeight("tufted_duck", 30), SpeciesWeight("red_crested_pochard", 25),
            SpeciesWeight("egyptian_goose", 20), SpeciesWeight("coot", 15), SpeciesWeight("pochard", 10),
        )
        6 -> listOf(
            SpeciesWeight("snow_goose", 35), SpeciesWeight("long_tailed_duck", 25),
            SpeciesWeight("goldeneye", 20), SpeciesWeight("barnacle", 10), SpeciesWeight("greylag", 10),
        )
        7 -> listOf(
            SpeciesWeight("great_crested_grebe", 30), SpeciesWeight("red_breasted_merganser", 25),
            SpeciesWeight("white_headed_duck", 20), SpeciesWeight("tufted_duck", 15), SpeciesWeight("coot", 10),
        )
        8 -> listOf(
            SpeciesWeight("mandarin", 30), SpeciesWeight("wood_duck", 25),
            SpeciesWeight("ferruginous_duck", 20), SpeciesWeight("tufted_duck", 15), SpeciesWeight("pochard", 10),
        )
        9 -> listOf(
            SpeciesWeight("merganser", 30), SpeciesWeight("scoter", 25),
            SpeciesWeight("tufted_duck", 20), SpeciesWeight("red_breasted_merganser", 15), SpeciesWeight("coot", 10),
        )
        10 -> listOf(
            SpeciesWeight("eider", 30), SpeciesWeight("brent_goose", 25),
            SpeciesWeight("scoter", 20), SpeciesWeight("long_tailed_duck", 15), SpeciesWeight("goldeneye", 10),
        )
        11 -> listOf(
            SpeciesWeight("golden", 15), SpeciesWeight("eider", 20), SpeciesWeight("scoter", 20),
            SpeciesWeight("merganser", 15), SpeciesWeight("red_crested_pochard", 10),
            SpeciesWeight("goldeneye", 10), SpeciesWeight("tufted_duck", 10),
        )
        12 -> regularById.values.map { SpeciesWeight(it.id, 10) }
        else -> error("Place must be 1..12")
    }

    private fun protectedPool(place: Int): List<SpeciesWeight> = buildList {
        add(SpeciesWeight("white_stork", 25))
        add(SpeciesWeight("heron", 25))
        add(SpeciesWeight("little_egret", 20))
        if (place >= 7) {
            add(SpeciesWeight("spoonbill", 20))
            add(SpeciesWeight("flamingo", 20))
        }
        if (place >= 9) add(SpeciesWeight("crane", 20))
    }
}
