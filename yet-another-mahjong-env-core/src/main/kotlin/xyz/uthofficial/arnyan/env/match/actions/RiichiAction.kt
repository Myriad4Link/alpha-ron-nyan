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

object RiichiAction : Action {
    override val id = Action.ID_RIICHI
    override fun toString() = "RIICHI"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        if (lastAction.player != actor) return false

        if (actor.seat != observation.currentSeatWind) return false
        if (actor.openHand.isNotEmpty()) return false
        if (actor.isRiichiDeclared) return false
        if (actor.score < 1000) return false

        return actor.closeHand.any { discardCandidate ->
            isInTenpai(actor.closeHand, discardCandidate)
        }
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
                        ErrorMessages.NO_DRAW_TO_TSUMO
                    ).wrapActionError()
                ).bind()
            }
            if (lastAction.player != actor) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.ONLY_DRAWING_PLAYER_CAN_TSUMO
                    ).wrapActionError()
                ).bind()
            }
            if (!actor.closeHand.contains(subject)) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.TILE_NOT_IN_HAND
                    ).wrapActionError()
                ).bind()
            }
            if (actor.openHand.isNotEmpty()) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.HAND_NOT_CLOSED
                    ).wrapActionError()
                ).bind()
            }
            if (actor.isRiichiDeclared) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.RIICHI_ALREADY_DECLARED
                    ).wrapActionError()
                ).bind()
            }
            if (actor.score < 1000) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.INSUFFICIENT_POINTS
                    ).wrapActionError()
                ).bind()
            }

            val tenpaiWaitingTiles = getTenpaiWaitingTiles(actor.closeHand - subject)
            if (tenpaiWaitingTiles.isEmpty()) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.NOT_IN_TENPAI
                    ).wrapActionError()
                ).bind()
            }

            val discardTile = subject

            val stateChanges = mutableListOf<StateChange>()
            stateChanges.add(StateChange.RemoveTilesFromHand(actorSeat, listOf(discardTile)))
            stateChanges.add(StateChange.UpdatePlayerScore(actorSeat, -1000))
            stateChanges.add(StateChange.UpdateRiichiSticks(1))

            val currentDiscards = observation.discards.toMutableMap()
            val playerDiscards = currentDiscards[actorSeat]?.toMutableList() ?: mutableListOf()
            playerDiscards.add(discardTile)
            currentDiscards[actorSeat] = playerDiscards

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = currentDiscards,
                lastAction = LastAction.Riichi(discardTile, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks + 1,
                honbaSticks = observation.honbaSticks
            )

            val nextWind = observation.topology.getShimocha(actorSeat).wrapActionError().bind()

            StepResult(newObservation, nextWind, false, stateChanges)
        }
}
