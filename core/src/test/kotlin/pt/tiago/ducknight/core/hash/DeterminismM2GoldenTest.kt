package pt.tiago.ducknight.core.hash

import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.replay.InputLog
import pt.tiago.ducknight.core.replay.Replay
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank

/**
 * Fifty whole-level determinism goldens across every M2 type and modifier.
 * The table is locked after the first run; changing a rule requires a deliberate update and a
 * SIM_VERSION decision.
 */
class DeterminismM2GoldenTest {
    private data class Golden(
        val name: String,
        val spec: LevelSpec,
        val options: GameOptions,
        val log: InputLog,
        val ticks: Int,
    )

    private fun rules(modifiers: Set<Modifier>): TypeRules = TypeRules(
        fogBanks = if (Modifier.FOG in modifiers) listOf(FogBank(60, 40), FogBank(150, 50)) else emptyList(),
        coverBoxes = if (Modifier.COVER in modifiers) listOf(CoverBox(20, 180, 60, 60)) else emptyList(),
        windStrength = 12,
    )

    private val modifierRotation = listOf(
        emptySet(),
        setOf(Modifier.WIND),
        setOf(Modifier.FOG),
        setOf(Modifier.RAIN),
        setOf(Modifier.SNOW),
        setOf(Modifier.COVER),
        setOf(Modifier.NIGHT),
        setOf(Modifier.GOLDEN),
        setOf(Modifier.MANUAL_ADVANCE),
    )

    private fun logFor(modifiers: Set<Modifier>): InputLog {
        var log = InputLog().append(1, InputAction.Snap(1, 1)).append(2, InputAction.Snap(3, 3))
        if (Modifier.MANUAL_ADVANCE in modifiers) {
            log = log.append(3, InputAction.Advance).append(4, InputAction.Snap(5, 5))
        }
        return log
    }

    private fun cases(): List<Golden> {
        val cases = mutableListOf<Golden>()
        for (i in 0 until 8) {
            val mods = modifierRotation[i]
            val spec = LevelSpec.classic(
                seed = 2_001L + i,
                birds = 3,
                need = 2,
                difficulty = Difficulty.entries[i % 3],
            ).copy(modifiers = mods, rules = if (mods.isEmpty()) null else rules(mods))
            cases += Golden("classic-$i", spec, GameOptions(), logFor(mods), 260)
        }
        for (i in 0 until 10) {
            val mods = modifierRotation[i % modifierRotation.size]
            val spec = LevelSpec.double(
                seed = 2_101L + i,
                birds = 4,
                need = 2,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = rules(mods),
            )
            cases += Golden("double-$i", spec, GameOptions(tolerancePercent = if (i % 2 == 0) 100 else 110), logFor(mods), 300)
        }
        for (i in 0 until 8) {
            val mods = modifierRotation[i % modifierRotation.size]
            val spec = LevelSpec.flock(
                seed = 2_201L + i,
                flocks = 2,
                flockSize = 4,
                need = 3,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = rules(mods).copy(flocks = 2, flockSize = 4),
            )
            cases += Golden("flock-$i", spec, GameOptions(biggerBirds = i % 3 == 0), logFor(mods), 320)
        }
        for (i in 0 until 6) {
            val mods = modifierRotation[i % modifierRotation.size]
            val spec = LevelSpec.film(
                seed = 2_301L + i,
                birds = 3,
                filmFrames = 5,
                need = 2,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = rules(mods),
            )
            cases += Golden("film-$i", spec, GameOptions(), logFor(mods), 320)
        }
        for (i in 0 until 6) {
            val mods = modifierRotation[i % modifierRotation.size]
            val spec = LevelSpec.timed(
                seed = 2_401L + i,
                seconds = 3,
                need = 1,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = rules(mods).copy(timedTicks = 180),
            )
            cases += Golden("timed-$i", spec, GameOptions(), logFor(mods), 240)
        }
        for (i in 0 until 6) {
            val mods = modifierRotation[i % modifierRotation.size]
            val spec = LevelSpec.protect(
                seed = 2_501L + i,
                birds = 3,
                protectedBirds = 1,
                need = 2,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = rules(mods).copy(protectedBirds = 1),
            )
            cases += Golden("protect-$i", spec, GameOptions(), logFor(mods), 360)
        }
        for (i in 0 until 4) {
            val mods = if (i == 3) setOf(Modifier.MANUAL_ADVANCE) else emptySet()
            val spec = LevelSpec.clay(
                seed = 2_601L + i,
                pairs = 2,
                need = 1,
                difficulty = Difficulty.entries[i % 3],
                modifiers = mods,
                rules = TypeRules(clayPairs = 2).let { if (mods.isEmpty()) it else it },
            )
            cases += Golden("clay-$i", spec, GameOptions(), logFor(mods), 300)
        }
        for (i in 0 until 2) {
            val spec = LevelSpec.boss(
                seed = 2_701L + i,
                pages = 3,
                difficulty = Difficulty.entries[i % 3],
            )
            cases += Golden("boss-$i", spec, GameOptions(), logFor(emptySet()), 300)
        }
        return cases
    }

    private val expected: Map<String, Long> = mapOf(
            "classic-0" to 37686110985756929L,
            "classic-1" to 7238681467315061082L,
            "classic-2" to 6939296310068675822L,
            "classic-3" to 8274798725794989399L,
            "classic-4" to -3780016153707899716L,
            "classic-5" to -682692806001876310L,
            "classic-6" to -4430958580862549865L,
            "classic-7" to 4414027390488334172L,
            "double-0" to 8955773835084118853L,
            "double-1" to 8610751637419559939L,
            "double-2" to 4565654320125621686L,
            "double-3" to -7518200420630677652L,
            "double-4" to 6009018100888142614L,
            "double-5" to 171487673063318249L,
            "double-6" to -5024990021745794576L,
            "double-7" to -5721108282260343557L,
            "double-8" to 4717079213084121677L,
            "double-9" to -2370913054973771304L,
            "flock-0" to 7399879787296450168L,
            "flock-1" to -2715215928856646353L,
            "flock-2" to 5027470701105842994L,
            "flock-3" to 8814961589889960143L,
            "flock-4" to 4889693662529151414L,
            "flock-5" to 5068403725229931317L,
            "flock-6" to -8118817988737291079L,
            "flock-7" to -885159949664435346L,
            "film-0" to -416650629450358830L,
            "film-1" to 258178297300985604L,
            "film-2" to 1551966199826736319L,
            "film-3" to 5218753672197635988L,
            "film-4" to 7855042953504121326L,
            "film-5" to -4213713109290527715L,
            "timed-0" to -1808271632606124883L,
            "timed-1" to -1269180433855256605L,
            "timed-2" to -3409085013669986272L,
            "timed-3" to 1862413687293421050L,
            "timed-4" to -6654990067752969318L,
            "timed-5" to -1360765490379436758L,
            "protect-0" to -7357854179485855895L,
            "protect-1" to 642563352581844837L,
            "protect-2" to 8271543887614257453L,
            "protect-3" to -1565632296799960069L,
            "protect-4" to -6850451556262958L,
            "protect-5" to -9098288551929781141L,
            "clay-0" to 1190022205093316086L,
            "clay-1" to 4406871932051809700L,
            "clay-2" to 2711513693451800444L,
            "clay-3" to -2870228832826990227L,
            "boss-0" to -925369979330392594L,
            "boss-1" to 5631365426740534175L
    )

    @Test
    fun `fifty goldens are locked`() {
        val cases = cases()
        org.junit.Assert.assertEquals(50, cases.size)
        for (case in cases) {
            val hash = Replay.runWithHash(case.spec, case.options, case.log, case.ticks).hash
            org.junit.Assert.assertEquals(case.name, expected.getValue(case.name), hash)
        }
    }


}
