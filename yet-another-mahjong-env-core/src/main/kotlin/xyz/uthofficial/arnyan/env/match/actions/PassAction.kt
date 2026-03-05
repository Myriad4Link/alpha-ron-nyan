package xyz.uthofficial.arnyan.env.match.actions

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.ErrorMessages
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.MatchState
import xyz.uthofficial.arnyan.env.match.StepResult
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.WinningMethod

object PassAction : Action {
    override val id = Action.ID_PASS
    override fun toString() = "PASS"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false

        if (lastAction.tile != subject) return false

        return true
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val seat = actor.seat
            if (seat == null) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        StandardWind.EAST,
                        ErrorMessages.PLAYER_HAS_NO_SEAT
                    ).wrapActionError()
                ).bind()
            }
            val seatWind: Wind = seat

            val lastAction = observation.lastAction
            if (lastAction !is LastAction.Discard) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        seatWind,
                        ErrorMessages.noDiscardToAction("pass")
                    ).wrapActionError()
                ).bind()
            }
            if (lastAction.tile != subject) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        seatWind,
                        ErrorMessages.TILE_MISMATCH
                    ).wrapActionError()
                ).bind()
            }

            val state = observation.toState()
            
            if (canWin(observation, actor, subject, WinningMethod.RON)) {
                if (actor.isRiichiDeclared) {
                    state.furitenPlayers.add(seatWind)
                } else {
                    state.temporaryFuritenPlayers.add(seatWind)
                }
            }

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = observation.currentSeatWind,
                roundRotationStatus = observation.roundRotationStatus,
                discards = observation.discards,
                lastAction = observation.lastAction,
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks,
                furitenPlayers = state.furitenPlayers.toSet(),
                temporaryFuritenPlayers = state.temporaryFuritenPlayers.toSet()
            )

            StepResult(newObservation, observation.currentSeatWind, false, emptyList())
        }
}
