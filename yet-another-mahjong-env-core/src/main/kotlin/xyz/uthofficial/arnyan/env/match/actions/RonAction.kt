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
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.YakuContext

object Ron : Action {
    override val id = Action.ID_RON
    override fun toString() = "RON"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false

        if (lastAction.player == actor) return false

        val actorSeat = actor.seat ?: return false
        
        if (actorSeat in observation.furitenPlayers) return false
        
        if (actorSeat in observation.temporaryFuritenPlayers) return false

        val actorDiscards = observation.discards[actorSeat] ?: emptyList()
        if (subject in actorDiscards) return false

        return canWin(observation, actor, subject, WinningMethod.RON)
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
                        ErrorMessages.noDiscardToAction("ron")
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
                        ErrorMessages.cannotActionOwnDiscard("ron")
                    ).wrapActionError()
                ).bind()
            }

            val partitions = resolvePartitions(actor.closeHand, subject)
            if (partitions.isEmpty()) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.HAND_NOT_COMPLETE
                    ).wrapActionError()
                ).bind()
            }
            val openMentsus = actor.openHand.map { tileGroupToMentsu(it, isOpen = true) }
                .map { it.raw }.toLongArray()
            val seatWind = actor.seat ?: StandardWind.EAST
            val roundWind = observation.roundRotationStatus.place
            val isOpenHand = actor.openHand.isNotEmpty()
            val isRiichiDeclared = actor.isRiichiDeclared
            val context = YakuContext(
                seatWind = seatWind,
                roundWind = roundWind,
                isOpenHand = isOpenHand,
                isRiichiDeclared = isRiichiDeclared,
                winningTile = subject,
                winningMethod = WinningMethod.RON
            )
            val maxHan = computeMaxHan(observation.yakuConfiguration, context, partitions, openMentsus)
            if (maxHan == 0) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.HAND_HAS_NO_YAKU
                    ).wrapActionError()
                ).bind()
            }

            val currentDiscards = observation.discards.toMutableMap()
            val playerDiscards = currentDiscards[discardingSeat]?.toMutableList() ?: mutableListOf()
            val lastIndex = playerDiscards.indexOfLast { it == subject }
            if (lastIndex != -1) {
                playerDiscards.removeAt(lastIndex)
            }
            currentDiscards[discardingSeat] = playerDiscards

            val scoringChanges = computeScoringStateChanges(
                observation = observation,
                actor = actor,
                subject = subject,
                winningMethod = WinningMethod.RON,
                partitions = partitions,
                discardingSeat = discardingSeat
            )
            val stateChanges = listOf(
                StateChange.RemoveTileFromDiscards(discardingSeat, subject)
            ) + scoringChanges

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = currentDiscards,
                lastAction = LastAction.Ron(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, true, stateChanges)
        }
}
