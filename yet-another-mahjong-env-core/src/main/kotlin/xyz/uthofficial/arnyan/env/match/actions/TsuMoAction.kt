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
import xyz.uthofficial.arnyan.env.yaku.WinningMethod

object TsuMo : Action {
    override val id = Action.ID_TSUMO
    override fun toString() = "TSU_MO"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        if (lastAction.tile != subject) return false

        if (lastAction.player != actor) return false

        return canWin(observation, actor, subject, WinningMethod.TSUMO)
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val lastAction = observation.lastAction
            val actorSeat = actor.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT)
                    .wrapActionError()
            ).bind()

            if (lastAction !is LastAction.Draw) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.NO_DRAW_TO_TSUMO
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

            if (lastAction.player != actor) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        ErrorMessages.ONLY_DRAWING_PLAYER_CAN_TSUMO
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
            val context = xyz.uthofficial.arnyan.env.yaku.YakuContext(
                seatWind = seatWind,
                roundWind = roundWind,
                isOpenHand = isOpenHand,
                isRiichiDeclared = isRiichiDeclared,
                winningTile = subject,
                winningMethod = WinningMethod.TSUMO
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

            val scoringChanges = computeScoringStateChanges(
                observation = observation,
                actor = actor,
                subject = subject,
                winningMethod = WinningMethod.TSUMO,
                partitions = partitions
            )

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = observation.discards,
                lastAction = LastAction.TsuMo(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, true, scoringChanges)
        }
}
