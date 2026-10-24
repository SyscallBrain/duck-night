package pt.tiago.ducknight.core.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Player actions. Coordinates are whole world units. */
@Serializable
sealed interface InputAction {
    @Serializable
    @SerialName("snap")
    data class Snap(val x: Int, val y: Int) : InputAction

    @Serializable
    @SerialName("advance")
    data object Advance : InputAction

    @Serializable
    @SerialName("aim_assist")
    data class AimAssist(val dx: Int, val dy: Int) : InputAction

    @Serializable
    @SerialName("pause")
    data object Pause : InputAction

    @Serializable
    @SerialName("resume")
    data object Resume : InputAction
}
