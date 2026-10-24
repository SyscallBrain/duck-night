package pt.tiago.ducknight.core.engine

import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdState

fun BirdState.aim(): InputAction.Snap = InputAction.Snap(toUnits(x), toUnits(y))

fun GameState.step(vararg inputs: InputAction): StepResult =
    pt.tiago.ducknight.core.engine.Simulation.step(this, inputs.toList())

fun GameState.runUntil(
    maxTicks: Int = 30_000,
    predicate: (GameState) -> Boolean,
): Pair<GameState, List<GameEvent>> {
    var state = this
    val events = mutableListOf<GameEvent>()
    var guard = 0
    while (!predicate(state) && guard < maxTicks) {
        val result = state.step()
        state = result.state
        events += result.events
        guard++
    }
    return state to events
}
