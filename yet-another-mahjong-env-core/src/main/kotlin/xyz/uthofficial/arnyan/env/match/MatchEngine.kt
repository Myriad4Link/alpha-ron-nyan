package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.getPlayerSitAt
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind

internal class MatchEngine(
    private val allActions: List<Action> = listOf(Chii, Pon, Ron, TsuMo, DiscardAction, PassAction)
) {
    fun maskToActions(mask: Int): List<Action> {
        if (mask == 0) return emptyList()
        val result = mutableListOf<Action>()
        allActions.forEach { action ->
            if (action.id and mask != 0) {
                result.add(action)
            }
        }
        return result
    }
    
    fun updateAvailableActions(state: MatchState) {
        val obs = state.toObservation()
        
        // Reset all player masks
        state.topology.seats.forEach { seat ->
            state.availableActionsMaskPerPlayer[seat] = 0
        }
        
        // Compute masks per player
        state.players.forEach { player ->
            val seat = player.seat ?: return@forEach
            var playerMask = 0
            
            // If player has already passed on the current discard, they cannot call interrupts
            val canCallInterrupts = seat !in state.passedPlayers
            
            allActions.forEach { action ->
                when (action) {
                    DiscardAction -> {
                        if (seat == state.currentSeatWind) {
                            // Discard is available if player has any tile in hand
                            player.closeHand.forEach { tile ->
                                if (action.availableWhen(obs, player, tile)) {
                                    playerMask = playerMask or action.id
                                    return@forEach
                                }
                            }
                        }
                    }
                    Chii, Pon, Ron -> {
                        if (!canCallInterrupts) {
                            // Player already passed, cannot call interrupts
                            return@forEach
                        }
                        val subject = when (obs.lastAction) {
                            is LastAction.Discard -> (obs.lastAction as LastAction.Discard).tile
                            else -> null
                        }
                        if (subject != null && action.availableWhen(obs, player, subject)) {
                            playerMask = playerMask or action.id
                        }
                    }
                    TsuMo -> {
                        val subject = when (obs.lastAction) {
                            is LastAction.Draw -> (obs.lastAction as LastAction.Draw).tile
                            else -> null
                        }
                        if (subject != null && action.availableWhen(obs, player, subject)) {
                            playerMask = playerMask or action.id
                        }
                    }
                    PassAction -> {
                        // Pass is available when there's a discard and player has at least one interrupt action available
                        val subject = when (obs.lastAction) {
                            is LastAction.Discard -> (obs.lastAction as LastAction.Discard).tile
                            else -> null
                        }
                        if (subject != null && canCallInterrupts) {
                            // Check if player has any interrupt action (Chii, Pon, Ron) available
                            val hasInterrupt = allActions.any { it.id in listOf(Action.ID_CHII, Action.ID_PON, Action.ID_RON) &&
                                    it.availableWhen(obs, player, subject) }
                            if (hasInterrupt) {
                                playerMask = playerMask or action.id
                            }
                        }
                    }
                }
            }
            
            state.availableActionsMaskPerPlayer[seat] = playerMask
        }
        

    }
    
    fun validateAction(state: MatchState, player: Player, action: Action, subject: Tile): Result<Unit, ActionError> {
        val seat = player.seat
        if (seat == null || seat !in state.topology.seats) {
            return Result.Failure(MatchError.PlayerNotInMatch(seat ?: StandardWind.EAST).wrapActionError())
        }
        
        // For non-interrupt actions (Discard, TsuMo), player must be current
        when (action) {
            DiscardAction, TsuMo -> {
                if (seat != state.currentSeatWind) {
                    return Result.Failure(MatchError.NotPlayersTurn(seat, state.currentSeatWind).wrapActionError())
                }
            }
            else -> {
                // Chii, Pon, Ron are interrupt actions and don't require being current player
            }
        }

        // Get player's action mask
        val playerMask = state.availableActionsMaskPerPlayer[seat] ?: 0

        // Check if action is available for this player
        if ((action.id and playerMask) == 0) {
            return Result.Failure(MatchError.ActionNotAvailable(action.toString(), seat, null).wrapActionError())
        }
        
        // Final availability check with specific tile
        if (!action.availableWhen(state.toObservation(), player, subject)) {
            return Result.Failure(MatchError.ActionNotAvailable(action.toString(), seat, "Action not available with given tile").wrapActionError())
        }
        
        return Result.Success(Unit)
    }
    
    fun start(state: MatchState): Result<StepResult, ActionError> = binding {
        val drawnTile = state.wall.draw(1).wrapActionError().bind().first()
        val currentPlayer = state.players.getPlayerSitAt(state.currentSeatWind)
        currentPlayer.closeHand.add(drawnTile)
        state.lastAction = LastAction.Draw(drawnTile, currentPlayer)
        updateAvailableActions(state)
        val currentState = state.toObservation()
        StepResult(currentState, state.topology.getShimocha(state.currentSeatWind).wrapActionError().bind(), false)
    }
    
    private fun drawTile(state: MatchState): Result<Tile, ActionError> {
        return when (val drawResult = state.wall.draw(1).wrapActionError()) {
            is Result.Success -> {
                val tile = drawResult.value.first()
                val currentPlayer = state.players.getPlayerSitAt(state.currentSeatWind)
                currentPlayer.closeHand.add(tile)
                state.lastAction = LastAction.Draw(tile, currentPlayer)
                Result.Success(tile)
            }
            is Result.Failure -> {
                // Wall exhausted (or other wall error)
                // For now, treat any wall error as exhaustion
                Result.Failure(MatchError.WallExhausted.wrapActionError())
            }
        }
    }
    
    private fun hasInterruptActionsAvailable(state: MatchState): Boolean {
        val interruptMask = Action.ID_CHII or Action.ID_PON or Action.ID_RON
        return state.availableActionsMaskPerPlayer.values.any { mask -> mask and interruptMask != 0 }
    }
    
    private fun hasRonAvailable(state: MatchState): Boolean {
        return state.availableActionsMaskPerPlayer.values.any { mask -> mask and Action.ID_RON != 0 }
    }
    
    private fun hasChiiPonAvailable(state: MatchState): Boolean {
        val mask = Action.ID_CHII or Action.ID_PON
        return state.availableActionsMaskPerPlayer.values.any { it and mask != 0 }
    }
    
    private fun getInterruptMaskForPlayer(state: MatchState, seat: Wind): Int {
        val obs = state.toObservation()
        val player = state.players.getPlayerSitAt(seat)
        val lastAction = obs.lastAction
        if (lastAction !is LastAction.Discard) return 0
        val subject = lastAction.tile
        var mask = 0
        if (Chii.availableWhen(obs, player, subject)) mask = mask or Action.ID_CHII
        if (Pon.availableWhen(obs, player, subject)) mask = mask or Action.ID_PON
        if (Ron.availableWhen(obs, player, subject)) mask = mask or Action.ID_RON
        return mask
    }
    
    private fun isInterruptPhase(state: MatchState): Boolean {
        // At least one player has interrupt actions available and hasn't passed
        state.topology.seats.forEach { seat ->
            if (seat !in state.passedPlayers) {
                val mask = getInterruptMaskForPlayer(state, seat)
                if (mask != 0) return true
            }
        }
        return false
    }
    
    private fun allInterruptsResolved(state: MatchState): Boolean {
        // For each player, either they have no interrupt actions available or they have passed
        state.topology.seats.forEach { seat ->
            if (seat !in state.passedPlayers) {
                val mask = getInterruptMaskForPlayer(state, seat)
                if (mask != 0) return false
            }
        }
        return true
    }
    
    fun submitAction(state: MatchState, player: Player, action: Action, subject: Tile): Result<StepResult, ActionError> = binding {
        validateAction(state, player, action, subject).bind()
        
        val stepResult = action.perform(state.toObservation(), player, subject).bind()
        
        // Apply state changes
        stepResult.stateChanges.forEach { change ->
            when (change) {
                is StateChange.RemoveTilesFromHand -> {
                    val targetPlayer = state.players.getPlayerSitAt(change.seat)
                    change.tiles.forEach { tile -> targetPlayer.closeHand.remove(tile) }
                }
                is StateChange.AddOpenGroup -> {
                    val targetPlayer = state.players.getPlayerSitAt(change.seat)
                    targetPlayer.openHand.add(change.group)
                }
                is StateChange.RemoveTileFromDiscards -> {
                    val playerDiscards = state.discards[change.seat]
                    if (playerDiscards != null) {
                        // Remove the last occurrence (most recent discard)
                        val lastIndex = playerDiscards.indexOfLast { it == change.tile }
                        if (lastIndex != -1) {
                            playerDiscards.removeAt(lastIndex)
                        }
                    }
                }
            }
        }
        
        // Update passedPlayers based on action
        when (action) {
            DiscardAction -> {
                // New discard, clear passed players
                state.passedPlayers.clear()
            }
            PassAction -> {
                // Player passes on current discard
                val seat = player.seat
                if (seat != null) {
                    state.passedPlayers.add(seat)
                }
            }
            Chii, Pon -> {
                // Interrupt called, clear passed players (interrupt resolved)
                state.passedPlayers.clear()
            }
            else -> {
                // Ron, TsuMo: game ends, no need to clear
            }
        }
        
        state.currentSeatWind = stepResult.nextWind
        state.roundRotationStatus = stepResult.observation.roundRotationStatus
        state.lastAction = stepResult.observation.lastAction
        state.discards.clear()
        stepResult.observation.discards.forEach { (wind, tiles) ->
            state.discards[wind] = tiles.toMutableList()
        }
        updateAvailableActions(state)
        
        // Determine if we need to advance turn after discard (all interrupts resolved)
        val finalStepResult = if (!stepResult.isOver && state.lastAction is LastAction.Discard && allInterruptsResolved(state)) {
            // All players have either passed or have no interrupts available
            // Advance turn to shimocha and draw tile
            val nextWind = state.topology.getShimocha(state.currentSeatWind).wrapActionError().bind()
            state.currentSeatWind = nextWind
            when (val drawResult = drawTile(state)) {
                is Result.Success -> {
                    updateAvailableActions(state)
                    StepResult(state.toObservation(), state.currentSeatWind, false)
                }
                is Result.Failure -> {
                    // Wall exhausted, game over (exhaustive draw)
                    StepResult(state.toObservation(), state.currentSeatWind, true)
                }
            }
        } else {
            stepResult
        }
        finalStepResult
    }
    
    fun checkOver(state: MatchState): Boolean {
        // Wall exhaustion
        if (state.wall.size == 0) return true
        // TODO: other end conditions (four kans, four riichi, etc.)
        return false
    }
}