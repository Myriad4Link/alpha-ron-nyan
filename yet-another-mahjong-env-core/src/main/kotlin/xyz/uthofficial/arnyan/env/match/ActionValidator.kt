package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.player.getPlayerSitAt
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.Minkan
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.Ron

internal class ActionValidator {
    fun validateAction(state: MatchState, player: Player, action: Action, subject: Tile): Result<Unit, ActionError> {
        val seat = player.seat
        if (seat == null || seat !in state.topology.seats) {
            return Result.Failure(MatchError.PlayerNotInMatch(seat ?: StandardWind.EAST).wrapActionError())
        }

        when (action) {
            xyz.uthofficial.arnyan.env.match.actions.DiscardAction, xyz.uthofficial.arnyan.env.match.actions.TsuMo -> {
                if (seat != state.currentSeatWind) {
                    return Result.Failure(MatchError.NotPlayersTurn(seat, state.currentSeatWind).wrapActionError())
                }
            }
            else -> {
            }
        }

        val playerMask = state.availableActionsMaskPerPlayer[seat] ?: 0

        if ((action.id and playerMask) == 0) {
            return Result.Failure(MatchError.ActionNotAvailable(action.toString(), seat, null).wrapActionError())
        }

        if (!action.availableWhen(state.toObservation(), player, subject)) {
            return Result.Failure(
                MatchError.ActionNotAvailable(
                    action.toString(),
                    seat,
                    "Action not available with given tile"
                ).wrapActionError()
            )
        }

        return Result.Success(Unit)
    }

    fun getInterruptMaskForPlayer(state: MatchState, seat: Wind): Int {
        val obs = state.toObservation()
        val player = state.players.getPlayerSitAt(seat)
        val lastAction = obs.lastAction
        if (lastAction !is LastAction.Discard) return 0
        val subject = lastAction.tile
        var mask = 0
        if (Chii.availableWhen(obs, player, subject)) mask = mask or Action.ID_CHII
        if (Pon.availableWhen(obs, player, subject)) mask = mask or Action.ID_PON
        if (Ron.availableWhen(obs, player, subject)) mask = mask or Action.ID_RON
        if (Minkan.availableWhen(obs, player, subject)) mask = mask or Action.ID_MINKAN
        return mask
    }

    fun isInterruptPhase(state: MatchState): Boolean {
        state.topology.seats.forEach { seat ->
            if (seat !in state.passedPlayers) {
                val mask = getInterruptMaskForPlayer(state, seat)
                if (mask != 0) return true
            }
        }
        return false
    }

    fun allInterruptsResolved(state: MatchState): Boolean {
        state.topology.seats.forEach { seat ->
            if (seat !in state.passedPlayers) {
                val mask = getInterruptMaskForPlayer(state, seat)
                if (mask != 0) return false
            }
        }
        return true
    }

    fun getInterruptPriorityOrder(discardingSeat: Wind, topology: TableTopology): List<Wind> {
        val seats = topology.seats
        val startIndex = seats.indexOf(discardingSeat)
        require(startIndex != -1) { "Discarding seat $discardingSeat not in topology seats $seats" }
        val ordered = mutableListOf<Wind>()
        var current = discardingSeat
        repeat(seats.size - 1) {
            current = topology.getShimocha(current).getOrThrow()
            ordered.add(current)
        }
        return ordered
    }

    fun applyInterruptPriority(
        discardingSeat: Wind,
        topology: TableTopology,
        availability: Map<Wind, Int>
    ): Map<Wind, Int> {
        val priorityOrder = getInterruptPriorityOrder(discardingSeat, topology)
        val result = availability.toMutableMap()
        
        var ronAssigned = false
        for (seat in priorityOrder) {
            val mask = result[seat] ?: 0
            if (mask and Action.ID_RON != 0) {
                if (ronAssigned) {
                    result[seat] = mask and Action.ID_RON.inv()
                } else {
                    ronAssigned = true
                }
            }
        }
        
        var kanAssigned = false
        for (seat in priorityOrder) {
            val mask = result[seat] ?: 0
            if (mask and Action.ID_MINKAN != 0) {
                if (kanAssigned) {
                    result[seat] = mask and Action.ID_MINKAN.inv()
                } else {
                    if ((mask and Action.ID_RON) == 0) {
                        kanAssigned = true
                    } else {
                        result[seat] = mask and Action.ID_MINKAN.inv()
                    }
                }
            }
        }
        
        var ponAssigned = false
        for (seat in priorityOrder) {
            val mask = result[seat] ?: 0
            if (mask and Action.ID_PON != 0) {
                if (ponAssigned) {
                    result[seat] = mask and Action.ID_PON.inv()
                } else {
                    if ((mask and Action.ID_RON) == 0 && (mask and Action.ID_MINKAN) == 0) {
                        ponAssigned = true
                    } else {
                        result[seat] = mask and Action.ID_PON.inv()
                    }
                }
            }
        }
        
        return result
    }
}
