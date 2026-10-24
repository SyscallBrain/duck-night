package pt.tiago.ducknight.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pt.tiago.ducknight.core.engine.GameState

/** Read-only frame source for [pt.tiago.ducknight.ui.screens.game.GameCanvas]. */
class ReplaySession(initial: GameState) : GameSceneState {
    override var state by mutableStateOf(initial)
    override val pinkUntilTick: Int = 0
    override val dogCue: DogCue? = null
    override val lastAimX: Int? = null
    override val lastAimY: Int? = null
    override val aimUntilTick: Int = 0
    override val floater: Floater? = null

    override fun visibleFeathers(): List<Feather> = emptyList()
}
