package pt.tiago.ducknight.game

import pt.tiago.ducknight.core.level.Difficulty

enum class DuelGame { A, B, C }

/**
 * Pure best-of-N duel state. Both players receive [seedForRound], so the same level and the same
 * flight are played twice. Scores are compared per round; a tied round is a draw. The match ends
 * at the majority or after [bestOf] rounds, with total score as the final tie-break.
 */
class DuelMatch(
    val playerOneName: String,
    val playerTwoName: String,
    val bestOf: Int,
    val game: DuelGame,
    val difficulty: Difficulty,
) {
    init {
        require(bestOf == 3 || bestOf == 5) { "Duel must be best of 3 or 5" }
        require(playerOneName.isNotBlank() && playerTwoName.isNotBlank()) { "Both players need a name" }
    }

    var round: Int = 1
        private set
    var currentPlayer: Int = 0
        private set
    val scores = IntArray(2)
    val wins = IntArray(2)
    val totals = IntArray(2)
    var roundsPlayed: Int = 0
        private set
    var over: Boolean = false
        private set
    var winner: Int? = null
        private set

    val needed: Int get() = bestOf / 2 + 1

    fun playerName(index: Int): String = if (index == 0) playerOneName else playerTwoName

    /** Same value for both players during a round; changes between rounds. */
    fun seedForRound(): Long =
        9_100L + game.ordinal * 1_000L + round * 37L + difficulty.ordinal

    fun record(score: Int) {
        require(!over) { "The match is already over" }
        scores[currentPlayer] = score.coerceAtLeast(0)
        if (currentPlayer == 0) {
            currentPlayer = 1
            return
        }
        roundsPlayed++
        totals[0] += scores[0]
        totals[1] += scores[1]
        when {
            scores[0] > scores[1] -> wins[0]++
            scores[1] > scores[0] -> wins[1]++
        }
        if (wins[0] >= needed || wins[1] >= needed || roundsPlayed >= bestOf) {
            over = true
            winner = when {
                wins[0] != wins[1] -> if (wins[0] > wins[1]) 0 else 1
                totals[0] != totals[1] -> if (totals[0] > totals[1]) 0 else 1
                else -> null
            }
        } else {
            round++
            currentPlayer = 0
            scores.fill(0)
        }
    }

    fun newMatch(): DuelMatch =
        DuelMatch(playerOneName, playerTwoName, bestOf, game, difficulty)
}
