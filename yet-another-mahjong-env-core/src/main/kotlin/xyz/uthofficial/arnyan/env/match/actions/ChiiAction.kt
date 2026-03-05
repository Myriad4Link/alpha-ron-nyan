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

object Chii : Action {
    override val id = Action.ID_CHII
    override fun toString() = "CHII"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false

        val discardingPlayer = lastAction.player
        val discardingSeat = discardingPlayer.seat ?: return false
        val actorSeat = actor.seat ?: return false

        val kamicha = when (val result = observation.topology.getKamicha(discardingSeat)) {
            is Result.Success -> result.value
            is Result.Failure -> null
        } ?: return false
        if (actorSeat != kamicha) return false

        return findSequenceTiles(actor.closeHand, subject) != null
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
                        ErrorMessages.noDiscardToAction("chii")
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
                        ErrorMessages.cannotActionOwnDiscard("chii")
                    ).wrapActionError()
                ).bind()
            }

            val kamicha = observation.topology.getKamicha(discardingSeat).wrapActionError().bind()
            if (actorSeat != kamicha) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.CHII_ONLY_BY_KAMICHA
                    ).wrapActionError()
                ).bind()
            }

            val sequenceTiles = findSequenceTiles(actor.closeHand, subject)
                ?: Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.NO_SEQUENCE_POSSIBLE
                    ).wrapActionError()
                ).bind()

            val (tile1, tile2) = sequenceTiles

            val openGroup = listOf(tile1, tile2, subject).sortedBy { it.index() }
            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(actorSeat, listOf(tile1, tile2)),
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
                lastAction = LastAction.Chii(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, false, stateChanges)
        }
}
