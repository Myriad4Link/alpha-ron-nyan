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
import xyz.uthofficial.arnyan.env.wind.Wind

object DiscardAction : Action {
    override val id = Action.ID_DISCARD
    override fun toString() = "DISCARD"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        return actor.seat == observation.currentSeatWind && actor.closeHand.contains(subject)
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val seat = actor.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(
                    toString(),
                    StandardWind.EAST,
                    ErrorMessages.PLAYER_HAS_NO_SEAT
                ).wrapActionError()
            ).bind()
            if (seat != observation.currentSeatWind) {
                Result.Failure<ActionError>(
                    MatchError.NotPlayersTurn(seat, observation.currentSeatWind).wrapActionError()
                ).bind()
            }
            val seatWind: Wind = seat
            if (!actor.closeHand.contains(subject)) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        seatWind,
                        ErrorMessages.TILE_NOT_IN_HAND
                    ).wrapActionError()
                ).bind()
            }

            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(seatWind, listOf(subject))
            )

            val currentDiscards = observation.discards
            val newDiscards = currentDiscards.toMutableMap()
            newDiscards[seatWind] = currentDiscards.getOrDefault(seatWind, emptyList()) + subject

            val nextWind = seatWind

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = seatWind,
                roundRotationStatus = observation.roundRotationStatus,
                discards = newDiscards,
                lastAction = LastAction.Discard(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, nextWind, false, stateChanges)
        }
}
