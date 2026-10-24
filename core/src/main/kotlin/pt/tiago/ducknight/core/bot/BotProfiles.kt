package pt.tiago.ducknight.core.bot

/** How long a model is willing to wait for the wings-up pose before taking the photo. */
enum class PosePolicy { NEVER, SOMETIMES, ALMOST_ALWAYS, ALWAYS }

/**
 * One human model. Reaction is jittered with a fixed log-normal-shaped
 * table; the aim error is a sum of uniforms (a normal approximation) in whole world units.
 */
data class BotProfile(
    val id: String,
    /** Mean reaction in ticks at 60 Hz. */
    val reactionMeanTicks: Int,
    val reactionJitter: Boolean,
    /** Standard deviation of the aim error, in world units. */
    val aimSigmaUnits: Int,
    /** Fraction of the movement during the reaction that the model anticipates, in percent. */
    val anticipationPercent: Int,
    /** Chance of choosing a protected bird by mistake, in percent. */
    val protectedMistakePercent: Int,
    /** Delay before pulling the film lever, in ticks. */
    val advanceDelayTicks: Int,
    val posePolicy: PosePolicy,
) {
    init {
        require(reactionMeanTicks > 0)
        require(aimSigmaUnits >= 0)
        require(anticipationPercent in 0..100)
        require(protectedMistakePercent in 0..100)
        require(advanceDelayTicks >= 0)
    }
}

/**
 * BOT_VERSION 2 (24 Oct 2026): the aim error and the reaction were measured against a real player
 * on the phone. The first values (σ 7/5/4 units, 380/320/280 ms) aimed to within 1.5 mm and passed
 * almost every level; a finger on a moving bird is off by 3–4 mm, about 12–18 world units. The
 * models also follow the target with the eyes (see HumanPhotographer.track).
 */
object BotProfiles {
    val EASY = BotProfile(
        id = "EASY_PLAYER",
        reactionMeanTicks = 27, // 450 ms
        reactionJitter = true,
        aimSigmaUnits = 18,
        anticipationPercent = 50,
        protectedMistakePercent = 8,
        advanceDelayTicks = 27, // 450 ms
        posePolicy = PosePolicy.NEVER,
    )
    val MEDIUM = BotProfile(
        id = "MEDIUM_PLAYER",
        reactionMeanTicks = 24, // 400 ms
        reactionJitter = true,
        aimSigmaUnits = 15,
        anticipationPercent = 65,
        protectedMistakePercent = 5,
        advanceDelayTicks = 21, // 350 ms
        posePolicy = PosePolicy.SOMETIMES,
    )
    val HARD = BotProfile(
        id = "HARD_PLAYER",
        reactionMeanTicks = 21, // 350 ms
        reactionJitter = true,
        aimSigmaUnits = 12,
        anticipationPercent = 75,
        protectedMistakePercent = 3,
        advanceDelayTicks = 18, // 300 ms
        posePolicy = PosePolicy.ALMOST_ALWAYS,
    )
    val PERFECT = BotProfile(
        id = "PERFECT",
        reactionMeanTicks = 9, // 150 ms
        reactionJitter = false,
        aimSigmaUnits = 0,
        anticipationPercent = 100,
        protectedMistakePercent = 0,
        advanceDelayTicks = 0,
        posePolicy = PosePolicy.ALWAYS,
    )

    val humans: List<BotProfile> = listOf(EASY, MEDIUM, HARD)
    val all: List<BotProfile> = humans + PERFECT

    fun byId(id: String): BotProfile = all.firstOrNull { it.id == id }
        ?: error("Unknown photographer model: $id")
}
