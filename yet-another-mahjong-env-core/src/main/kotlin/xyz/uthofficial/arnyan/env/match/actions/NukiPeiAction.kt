package xyz.uthofficial.arnyan.env.match.actions

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.ErrorMessages
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.StateChange
import xyz.uthofficial.arnyan.env.match.StepResult
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind

object NukiPei : Action {
    override val id = Action.ID_NUKI
    override fun toString() = "NUKI_PEI"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        if (actor.seat != observation.currentSeatWind) return false
        
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        
        val hasNorthTile = actor.closeHand.any { it.tileType is xyz.uthofficial.arnyan.env.tile.Wind && it.value == 4 }
        if (!hasNorthTile) return false
        
        if (observation.wall.size == 0) return false
        
        return true
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val actorSeat = actor.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT)
                    .wrapActionError()
            ).bind()

            val lastAction = observation.lastAction
            if (lastAction !is LastAction.Draw) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "Nuki-Pei can only be performed after drawing a tile"
                    ).wrapActionError()
                ).bind()
            }

            val northTile = actor.closeHand.find { it.tileType is xyz.uthofficial.arnyan.env.tile.Wind && it.value == 4 }
                ?: Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "No North wind tile in hand"
                    ).wrapActionError()
                ).bind()

            actor.nukiCount++

            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(actorSeat, listOf(northTile)),
                StateChange.AddOpenGroup(actorSeat, listOf(northTile)),
                StateChange.DrawReplacementTile(actorSeat)
            )

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = observation.discards,
                lastAction = LastAction.AnKan(northTile, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, false, stateChanges)
        }
}
