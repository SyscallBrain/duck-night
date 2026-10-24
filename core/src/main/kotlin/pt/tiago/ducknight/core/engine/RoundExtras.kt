package pt.tiago.ducknight.core.engine

import pt.tiago.ducknight.core.fair.ClayPigeon
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank

/** Boss album and phase. A good photo fills one page and changes the flight. */
data class BossState(
    val phase: Int = 1,
    val pages: Int = 0,
    val photos: Int = 0,
    /** When the album last grew (or the round started): the Thief Crow counts from here. */
    val lastPageTick: Int = 0,
    /** Thunder: the V is broken until this tick, and the next lightning comes at [nextStormTick]. */
    val stormUntil: Int = 0,
    val nextStormTick: Int = 0,
    /** The Wind Goose has gathered its flock for the last page. */
    val escorts: Boolean = false,
)

/**
 * State that only the richer level types need. It is null for a plain CLASSIC level, so the original hashes
 * stay exactly as they were before these mechanics existed.
 */
data class RoundExtras(
    val rules: TypeRules,
    /** Shared roll for FILM; null when photos are counted per bird. */
    val filmLeft: Int? = null,
    /** TIMED ends here. */
    val timedEndTick: Int? = null,
    /** The next shutter is blocked until this tick (TIMED). */
    val cooldownUntil: Int = 0,
    val protectedPhotoCount: Int = 0,
    /** MANUAL_ADVANCE: the lever must be pulled before the next photo. */
    val needsAdvance: Boolean = false,
    /** GOLDEN consecutive good photos. */
    val combo: Int = 0,
    val multiplierPercent: Int = 100,
    /** Current wind force in fixed-point units per tick. */
    val windFx: Int = 0,
    val windNextTick: Int = 0,
    val windRngState: Long = 0,
    val fogBanks: List<FogBank> = emptyList(),
    val coverBoxes: List<CoverBox> = emptyList(),
    /** NIGHT: center of the lamp and the bird it follows for 0.4 s. */
    val nightX: Int = 120,
    val nightY: Int = 150,
    val nightFollowId: Int = -1,
    val nightFollowUntil: Int = 0,
    val boss: BossState? = null,
    val clays: List<ClayPigeon> = emptyList(),
    val clayPairsSpawned: Int = 0,
    val corksLeft: Int = 0,
    val nextClaySpawnTick: Int = 0,
    /** PROTECT: protected birds already spawned. */
    val protectedSpawned: Int = 0,
    val nextProtectedSpawnTick: Int = 0,
    /** FLOCK: the V has been disturbed and its members fly on their own. */
    val formationBroken: Boolean = false,
)
