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

object Ankan : Action {
    override val id = Action.ID_ANKAN
    override fun toString() = "ANKAN"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        if (actor.seat != observation.currentSeatWind) return false
        
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        
        val matchingTiles = findFourIdenticalTiles(actor.closeHand, subject)
        return matchingTiles != null
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
                        "Ankan can only be performed after drawing a tile"
                    ).wrapActionError()
                ).bind()
            }

            val fourTiles = findFourIdenticalTiles(actor.closeHand, subject)
                ?: Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "No four identical tiles for ankan"
                    ).wrapActionError()
                ).bind()

            val openGroup = fourTiles.sortedBy { it.index() }
            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(actorSeat, fourTiles),
                StateChange.AddOpenGroup(actorSeat, openGroup)
            )

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = observation.discards,
                lastAction = LastAction.Ankan(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, false, stateChanges)
        }
}

internal fun findFourIdenticalTiles(hand: List<Tile>, subject: Tile): List<Tile>? {
    val subjectIdx = subject.index()
    val matchingTiles = hand.filter { it.index() == subjectIdx }
    return if (matchingTiles.size >= 3) {
        matchingTiles.take(3) + subject
    } else {
        null
    }
}
