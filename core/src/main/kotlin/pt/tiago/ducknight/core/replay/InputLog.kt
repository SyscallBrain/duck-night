package pt.tiago.ducknight.core.replay

import kotlinx.serialization.Serializable
import pt.tiago.ducknight.core.engine.InputAction

/** One action at one simulation tick. */
@Serializable
data class InputLogEntry(val tick: Int, val action: InputAction) {
    init {
        require(tick >= 0) { "Input ticks must not be negative" }
    }
}

/**
 * Ordered record of the player's actions. Appending a tick after existing
 * entries is stable, so two actions on the same tick keep their real order.
 */
@Serializable
data class InputLog(val entries: List<InputLogEntry> = emptyList()) {
    init {
        require(entries.zipWithNext().all { (a, b) -> a.tick <= b.tick }) {
            "Input log must be ordered by tick"
        }
    }

    fun append(tick: Int, action: InputAction): InputLog {
        require(tick >= 0) { "Input ticks must not be negative" }
        val entry = InputLogEntry(tick, action)
        val copy = entries.toMutableList()
        var index = copy.size
        while (index > 0 && copy[index - 1].tick > tick) index--
        copy.add(index, entry)
        return this.copy(entries = copy)
    }

    fun actionsAt(tick: Int): List<InputAction> =
        entries.filter { it.tick == tick }.map { it.action }

    fun maxTick(): Int = entries.lastOrNull()?.tick ?: 0

    companion object {
        fun of(vararg entries: InputLogEntry): InputLog =
            InputLog(entries.sortedBy { it.tick })
    }
}
