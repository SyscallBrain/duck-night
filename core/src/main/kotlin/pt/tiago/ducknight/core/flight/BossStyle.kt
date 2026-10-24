package pt.tiago.ducknight.core.flight

/**
 * The twelve legendary birds, one mechanic each. The style comes from
 * the species, so a boss level needs no extra content fields; any other species keeps the plain
 * plain wandering boss.
 */
enum class BossStyle {
    OLD_MALLARD,
    TWIN_TEALS,
    WIND_GOOSE,
    MIST_GOOSE,
    THIEF_CROW,
    SNOW_GOOSE,
    ROYAL_GREBE,
    MANDARIN,
    GHOST,
    THUNDER_FLOCK,
    GOLDEN_EIDER,
    GREAT_DUCK,
    PLAIN,
    ;

    /** The Great Duck borrows one mechanic per album phase: mist, ghost, then the dive. */
    fun forPhase(phase: Int): BossStyle = if (this != GREAT_DUCK) {
        this
    } else {
        when (phase) {
            1 -> MIST_GOOSE
            2 -> GHOST
            else -> ROYAL_GREBE
        }
    }

    companion object {
        fun of(speciesId: String): BossStyle = when (speciesId) {
            "old_mallard" -> OLD_MALLARD
            "twin_teal" -> TWIN_TEALS
            "wind_goose" -> WIND_GOOSE
            "mist_goose" -> MIST_GOOSE
            "thief_crow" -> THIEF_CROW
            "royal_snow_goose" -> SNOW_GOOSE
            "royal_grebe" -> ROYAL_GREBE
            "imperial_mandarin" -> MANDARIN
            "ghost_duck" -> GHOST
            "thunder_flock" -> THUNDER_FLOCK
            "golden_eider" -> GOLDEN_EIDER
            "great_duck" -> GREAT_DUCK
            else -> PLAIN
        }

        /** After a page the legendary bird is out of sight for 1.5 s. */
        const val PAGE_REST_TICKS = 90

        /** Mist banks: visible ticks out of every [MIST_CYCLE], shrinking with each page. */
        const val MIST_CYCLE = 170

        fun mistVisibleTicks(phase: Int): Int = when (phase) {
            1 -> 110
            2 -> 80
            else -> 55
        }

        /** The ghost shows itself this many ticks before it changes course. */
        const val GHOST_TELL_TICKS = 24

        /** And stays visible this long after the turn. */
        const val GHOST_AFTER_TICKS = 40

        /** The snow goose melts into the snow for the last [SNOW_HIDDEN] ticks of every [SNOW_CYCLE]. */
        const val SNOW_CYCLE = 140
        const val SNOW_HIDDEN = 40

        /** The crow steals a page when the album has not grown for 3 s. */
        const val CROW_STEAL_TICKS = 180

        /** The grebe flies this long between dives. */
        const val GREBE_FLY_TICKS = 150

        fun grebeDiveTicks(phase: Int): Int = 45 + phase * 15

        /** Thunder: the V breaks for [STORM_TICKS] at every lightning. */
        const val STORM_TICKS = 75

        fun stormGap(phase: Int): Int = 260 - phase * 40

        /** A thunder-flock photo needs this many birds to fill a page. */
        const val THUNDER_GROUP = 3
    }
}
