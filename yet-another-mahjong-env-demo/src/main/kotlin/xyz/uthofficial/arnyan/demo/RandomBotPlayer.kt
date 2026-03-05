package xyz.uthofficial.arnyan.demo

import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.NukiPei
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo
import xyz.uthofficial.arnyan.env.player.Player
import kotlin.random.Random

object RandomBotPlayer {
    fun selectAction(observation: MatchObservation, player: Player): Pair<Action, Boolean> {
        val availableActions = observation.availableActions
        
        val winningActions = availableActions.filter { it == TsuMo || it == Ron }
        if (winningActions.isNotEmpty()) {
            return winningActions.first() to false
        }
        
        if (availableActions.contains(NukiPei)) {
            return NukiPei to false
        }
        
        val nonPassActions = availableActions.filter { it != PassAction && it != DiscardAction }
        if (nonPassActions.isNotEmpty()) {
            return nonPassActions.random() to false
        }
        
        val isDiscardTurn = player.seat == observation.currentSeatWind
        return if (isDiscardTurn && availableActions.contains(DiscardAction)) {
            DiscardAction to true
        } else {
            PassAction to false
        }
    }
    
    fun selectDiscardTile(player: Player): xyz.uthofficial.arnyan.env.tile.Tile {
        val hand = player.closeHand
        require(hand.isNotEmpty()) { "Player hand is empty" }
        
        val grouped = hand.groupingBy { it }.eachCount()
        val nonWinningTiles = hand.filter { tile ->
            val count = grouped[tile] ?: 1
            count < 4
        }
        
        return if (nonWinningTiles.isNotEmpty()) {
            nonWinningTiles.random()
        } else {
            hand.random()
        }
    }
}
