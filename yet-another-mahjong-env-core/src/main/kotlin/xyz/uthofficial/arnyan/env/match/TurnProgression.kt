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

internal class TurnProgression(
    private val actionMaskBuilder: ActionMaskBuilder,
    private val validator: ActionValidator
) {
    fun start(state: MatchState): Result<StepResult, ActionError> = binding {
        val drawnTile = state.wall.draw(1).wrapActionError().bind().first()
        val currentPlayer = state.players.getPlayerSitAt(state.currentSeatWind)
        currentPlayer.closeHand.add(drawnTile)
        state.lastAction = LastAction.Draw(drawnTile, currentPlayer)
        
        val currentPlayerSeat = currentPlayer.seat
        if (currentPlayerSeat != null) {
            state.temporaryFuritenPlayers.remove(currentPlayerSeat)
        }
        
        actionMaskBuilder.updateAvailableActions(state)
        val currentState = state.toObservation()
        StepResult(currentState, state.topology.getShimocha(state.currentSeatWind).wrapActionError().bind(), false)
    }

    fun drawTile(state: MatchState): Result<Tile, ActionError> {
        return when (val drawResult = state.wall.draw(1).wrapActionError()) {
            is Result.Success -> {
                val tile = drawResult.value.first()
                val currentPlayer = state.players.getPlayerSitAt(state.currentSeatWind)
                currentPlayer.closeHand.add(tile)
                state.lastAction = LastAction.Draw(tile, currentPlayer)
                Result.Success(tile)
            }

            is Result.Failure -> {
                Result.Failure(MatchError.WallExhausted.wrapActionError())
            }
        }
    }

    fun computeExhaustiveDrawStateChanges(state: MatchState): List<StateChange> {
        val playerWinds = state.topology.seats
        val dealerWind = playerWinds.firstOrNull { it == StandardWind.EAST } ?: playerWinds.first()
        val openHands = state.players.associate { player ->
            player.seat!! to player.openHand.map { it.toList() }
        }
        val payments = state.scoringCalculator.computeExhaustiveDrawPayments(
            playerWinds = playerWinds,
            discards = state.discards.mapValues { (_, list) -> list.toList() },
            openHands = openHands,
            riichiSticks = state.riichiSticks,
            honbaSticks = state.honbaSticks,
            dealerWind = dealerWind
        )
        val changes = mutableListOf<StateChange>()
        payments.forEach { (wind, delta) ->
            if (delta != 0) {
                changes.add(StateChange.UpdatePlayerScore(wind, delta))
            }
        }
        changes.add(StateChange.UpdateHonbaSticks(1))
        return changes
    }

    fun checkOver(state: MatchState): Boolean {
        if (state.wall.size == 0) return true
        
        val playerCount = state.topology.seats.size
        
        if (countTotalKans(state) >= playerCount) return true
        
        if (countRiichiDeclarations(state) >= playerCount) return true
        
        return false
    }

    private fun countTotalKans(state: MatchState): Int {
        var total = 0
        for (player in state.players) {
            total += player.openHand.count { it.size == 4 }
        }
        return total
    }

    private fun countRiichiDeclarations(state: MatchState): Int {
        return state.players.count { it.isRiichiDeclared }
    }

    fun applyActionStateChanges(
        state: MatchState,
        action: Action,
        player: Player,
        stepResult: StepResult
    ) {
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

                is StateChange.RemoveOpenGroup -> {
                    val targetPlayer = state.players.getPlayerSitAt(change.seat)
                    if (change.groupIndex in targetPlayer.openHand.indices) {
                        targetPlayer.openHand.removeAt(change.groupIndex)
                    }
                }

                is StateChange.RemoveTileFromDiscards -> {
                    val playerDiscards = state.discards[change.seat]
                    if (playerDiscards != null) {
                        val lastIndex = playerDiscards.indexOfLast { it == change.tile }
                        if (lastIndex != -1) {
                            playerDiscards.removeAt(lastIndex)
                        }
                    }
                }

                is StateChange.UpdatePlayerScore -> {
                    val targetPlayer = state.players.getPlayerSitAt(change.seat)
                    targetPlayer.score += change.delta
                }

                is StateChange.UpdateRiichiSticks -> {
                    state.riichiSticks += change.delta
                }

                is StateChange.UpdateHonbaSticks -> {
                    state.honbaSticks += change.delta
                }

                is StateChange.DrawReplacementTile -> {
                    val targetPlayer = state.players.getPlayerSitAt(change.seat)
                    val replacementResult = state.wall.draw(1)
                    if (replacementResult is xyz.uthofficial.arnyan.env.result.Result.Success) {
                        targetPlayer.closeHand.add(replacementResult.value.first())
                    }
                }
                
                is StateChange.RevealDoraIndicator -> {
                    state.doraIndicators.add(change.tile)
                }
            }
        }

        when (action) {
            xyz.uthofficial.arnyan.env.match.actions.DiscardAction -> {
                state.passedPlayers.clear()
            }

            xyz.uthofficial.arnyan.env.match.actions.PassAction -> {
                val seat = player.seat
                if (seat != null) {
                    state.passedPlayers.add(seat)
                }
            }

            xyz.uthofficial.arnyan.env.match.actions.RiichiAction -> {
                player.isRiichiDeclared = true
                player.riichiSticksDeposited = 1
                state.passedPlayers.clear()
                
                // Reveal lower tile on next stack as dora indicator
                val riichiDora = state.wall.revealRiichiDoraIndicator()
                if (riichiDora is xyz.uthofficial.arnyan.env.result.Result.Success) {
                    state.doraIndicators.add(riichiDora.value)
                }
            }

            xyz.uthofficial.arnyan.env.match.actions.Chii, 
            xyz.uthofficial.arnyan.env.match.actions.Pon -> {
                state.passedPlayers.clear()
            }

            xyz.uthofficial.arnyan.env.match.actions.AnKan,
            xyz.uthofficial.arnyan.env.match.actions.MinKan,
            xyz.uthofficial.arnyan.env.match.actions.KaKan -> {
                state.passedPlayers.clear()
                val playerSeat = player.seat
                if (playerSeat != null) {
                    state.temporaryFuritenPlayers.remove(playerSeat)
                }
                
                val newDoraIndicator = state.wall.revealNextDoraIndicator()
                if (newDoraIndicator is xyz.uthofficial.arnyan.env.result.Result.Success) {
                    state.doraIndicators.add(newDoraIndicator.value)
                }
            }
            
            xyz.uthofficial.arnyan.env.match.actions.NukiPei -> {
                state.passedPlayers.clear()
                val playerSeat = player.seat
                if (playerSeat != null) {
                    state.temporaryFuritenPlayers.remove(playerSeat)
                }
            }

            else -> {
            }
        }

        state.currentSeatWind = stepResult.nextWind
        state.roundRotationStatus = stepResult.observation.roundRotationStatus
        state.lastAction = stepResult.observation.lastAction
        state.discards.clear()
        stepResult.observation.discards.forEach { (wind, tiles) ->
            state.discards[wind] = tiles.toMutableList()
        }
        actionMaskBuilder.updateAvailableActions(state)
    }

    fun handlePostDiscardTurnAdvancement(state: MatchState): StepResult {
        return when (val drawResult = drawTile(state)) {
            is Result.Success -> {
                val currentPlayer = state.players.getPlayerSitAt(state.currentSeatWind)
                val currentPlayerSeat = currentPlayer.seat
                if (currentPlayerSeat != null) {
                    state.temporaryFuritenPlayers.remove(currentPlayerSeat)
                }
                actionMaskBuilder.updateAvailableActions(state)
                StepResult(state.toObservation(), state.currentSeatWind, false)
            }

            is Result.Failure -> {
                val exhaustiveDrawChanges = computeExhaustiveDrawStateChanges(state)
                exhaustiveDrawChanges.forEach { change ->
                    when (change) {
                        is StateChange.UpdatePlayerScore -> {
                            val targetPlayer = state.players.getPlayerSitAt(change.seat)
                            targetPlayer.score += change.delta
                        }
                        is StateChange.UpdateRiichiSticks -> {
                            state.riichiSticks += change.delta
                        }
                        is StateChange.UpdateHonbaSticks -> {
                            state.honbaSticks += change.delta
                        }
                        else -> {}
                    }
                }
                actionMaskBuilder.updateAvailableActions(state)
                StepResult(state.toObservation(), state.currentSeatWind, true)
            }
        }
    }
}
