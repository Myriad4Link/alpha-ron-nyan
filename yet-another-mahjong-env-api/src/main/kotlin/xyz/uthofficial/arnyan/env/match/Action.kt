package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Tile

interface Action {
    val id: Int

    fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean

    fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError>

    companion object {
        const val ID_CHII = 1 shl 0
        const val ID_PON = 1 shl 1
        const val ID_RON = 1 shl 2
        const val ID_TSUMO = 1 shl 3
        const val ID_DISCARD = 1 shl 4
        const val ID_PASS = 1 shl 5
        const val ID_RIICHI = 1 shl 6
        const val ID_ANKAN = 1 shl 7
        const val ID_MINKAN = 1 shl 8
        const val ID_KAKAN = 1 shl 9
    }
}