package pt.tiago.ducknight.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.Difficulty

class DuelMatchTest {
    @Test
    fun `best of three ends after three paired rounds`() {
        val match = DuelMatch("Ana", "João", bestOf = 3, game = DuelGame.A, difficulty = Difficulty.MEDIUM)
        assertEquals(0, match.currentPlayer)
        assertFalse(match.over)

        match.record(100)
        assertEquals(1, match.currentPlayer)
        assertFalse(match.over)

        match.record(80)
        assertEquals(1, match.wins[0])
        assertEquals(2, match.round)
        assertEquals(0, match.currentPlayer)

        match.record(30)
        match.record(40)
        assertEquals(1, match.wins[1])
        assertEquals(3, match.round)

        match.record(50)
        match.record(20)
        assertTrue(match.over)
        assertEquals(0, match.winner)
        assertEquals(2, match.wins[0])
        assertEquals(3, match.roundsPlayed)
    }

    @Test
    fun `both players get the same seed for a round`() {
        val match = DuelMatch("A", "B", bestOf = 3, game = DuelGame.C, difficulty = Difficulty.HARD)
        val first = match.seedForRound()
        match.record(10)
        val second = match.seedForRound()
        assertEquals(first, second)
        match.record(20)
        assertNotEquals(first, match.seedForRound())
    }

    @Test
    fun `best of five stops at the majority`() {
        val match = DuelMatch("A", "B", bestOf = 5, game = DuelGame.B, difficulty = Difficulty.EASY)
        repeat(3) {
            match.record(100)
            match.record(10)
        }
        assertTrue(match.over)
        assertEquals(0, match.winner)
        assertEquals(3, match.wins[0])
    }

    @Test
    fun `a draw on the scoreboard is decided by the total score`() {
        val match = DuelMatch("A", "B", bestOf = 3, game = DuelGame.C, difficulty = Difficulty.MEDIUM)
        match.record(10)
        match.record(20)
        match.record(30)
        match.record(30)
        match.record(40)
        match.record(10)
        assertTrue(match.over)
        assertEquals(1, match.wins[0])
        assertEquals(1, match.wins[1])
        assertEquals(0, match.winner)
    }

    @Test
    fun `a new match keeps the names and settings`() {
        val match = DuelMatch("Ana", "João", bestOf = 5, game = DuelGame.B, difficulty = Difficulty.HARD)
        val next = match.newMatch()
        assertEquals("Ana", next.playerOneName)
        assertEquals("João", next.playerTwoName)
        assertEquals(5, next.bestOf)
        assertEquals(DuelGame.B, next.game)
        assertEquals(Difficulty.HARD, next.difficulty)
        assertFalse(next.over)
        assertNull(next.winner)
    }
}
