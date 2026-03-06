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

object MinKan : Action {
    override val id = Action.ID_MINKAN
    override fun toString() = "MIN_KAN"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false
        if (lastAction.player == actor) return false

        val matchingTiles = findThreeIdenticalTiles(actor.closeHand, subject)
        return matchingTiles != null
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val lastAction = observation.lastAction
            val actorSeat = actor.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT)
                    .wrapActionError()
            ).bind()

            if (lastAction !is LastAction.Discard) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.noDiscardToAction("minkan")
                    ).wrapActionError()
                ).bind()
            }
            if (lastAction.tile != subject) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.TILE_MISMATCH
                    ).wrapActionError()
                ).bind()
            }

            val discardingPlayer = lastAction.player
            val discardingSeat = discardingPlayer.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT)
                    .wrapActionError()
            ).bind()
            if (discardingPlayer == actor) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.cannotActionOwnDiscard("minkan")
                    ).wrapActionError()
                ).bind()
            }

            val threeTiles = findThreeIdenticalTiles(actor.closeHand, subject)
                ?: Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "No three identical tiles for minkan"
                    ).wrapActionError()
                ).bind()

            val openGroup = (threeTiles + subject).sortedBy { it.index() }
            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(actorSeat, threeTiles),
                StateChange.AddOpenGroup(actorSeat, openGroup),
                StateChange.RemoveTileFromDiscards(discardingSeat, subject)
            )

            val currentDiscards = observation.discards.toMutableMap()
            val playerDiscards = currentDiscards[discardingSeat]?.toMutableList() ?: mutableListOf()
            val lastIndex = playerDiscards.indexOfLast { it == subject }
            if (lastIndex != -1) {
                playerDiscards.removeAt(lastIndex)
            }
            currentDiscards[discardingSeat] = playerDiscards

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = currentDiscards,
                lastAction = LastAction.MinKan(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, false, stateChanges)
        }
}

internal fun findThreeIdenticalTiles(hand: List<Tile>, subject: Tile): List<Tile>? {
    val subjectIdx = subject.index()
    val matchingTiles = hand.filter { it.index() == subjectIdx }
    return if (matchingTiles.size >= 3) {
        matchingTiles.take(3)
    } else {
        null
    }
}
