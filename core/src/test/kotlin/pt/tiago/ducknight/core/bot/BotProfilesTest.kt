package pt.tiago.ducknight.core.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BotProfilesTest {
    @Test
    fun `the three human models match the plan`() {
        assertEquals(27, BotProfiles.EASY.reactionMeanTicks)
        assertEquals(24, BotProfiles.MEDIUM.reactionMeanTicks)
        assertEquals(21, BotProfiles.HARD.reactionMeanTicks)
        assertEquals(18, BotProfiles.EASY.aimSigmaUnits)
        assertEquals(15, BotProfiles.MEDIUM.aimSigmaUnits)
        assertEquals(12, BotProfiles.HARD.aimSigmaUnits)
        assertEquals(50, BotProfiles.EASY.anticipationPercent)
        assertEquals(65, BotProfiles.MEDIUM.anticipationPercent)
        assertEquals(75, BotProfiles.HARD.anticipationPercent)
        assertEquals(8, BotProfiles.EASY.protectedMistakePercent)
        assertEquals(5, BotProfiles.MEDIUM.protectedMistakePercent)
        assertEquals(3, BotProfiles.HARD.protectedMistakePercent)
        assertEquals(27, BotProfiles.EASY.advanceDelayTicks)
        assertEquals(21, BotProfiles.MEDIUM.advanceDelayTicks)
        assertEquals(18, BotProfiles.HARD.advanceDelayTicks)
    }

    @Test
    fun `the perfect model reacts in 150 ms with no error`() {
        assertEquals(9, BotProfiles.PERFECT.reactionMeanTicks)
        assertEquals(0, BotProfiles.PERFECT.aimSigmaUnits)
        assertEquals(100, BotProfiles.PERFECT.anticipationPercent)
        assertEquals(PosePolicy.ALWAYS, BotProfiles.PERFECT.posePolicy)
    }

    @Test
    fun `the pose policies follow the plan`() {
        assertEquals(PosePolicy.NEVER, BotProfiles.EASY.posePolicy)
        assertEquals(PosePolicy.SOMETIMES, BotProfiles.MEDIUM.posePolicy)
        assertEquals(PosePolicy.ALMOST_ALWAYS, BotProfiles.HARD.posePolicy)
    }

    @Test
    fun `profiles can be looked up by id`() {
        assertEquals(BotProfiles.HARD, BotProfiles.byId("HARD_PLAYER"))
        assertTrue(runCatching { BotProfiles.byId("nobody") }.isFailure)
    }

    @Test
    fun `the profiles reject impossible values`() {
        assertTrue(runCatching { BotProfiles.EASY.copy(reactionMeanTicks = 0) }.isFailure)
        assertTrue(runCatching { BotProfiles.EASY.copy(aimSigmaUnits = -1) }.isFailure)
        assertTrue(runCatching { BotProfiles.EASY.copy(anticipationPercent = 101) }.isFailure)
        assertTrue(runCatching { BotProfiles.EASY.copy(protectedMistakePercent = 101) }.isFailure)
    }
}
