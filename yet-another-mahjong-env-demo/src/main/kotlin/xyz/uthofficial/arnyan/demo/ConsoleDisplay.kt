package xyz.uthofficial.arnyan.demo

import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import kotlin.collections.groupBy
import kotlin.collections.mapValues
import kotlin.collections.toList
import kotlin.collections.sortedBy
import kotlin.collections.joinToString

object ConsoleDisplay {
    fun printGameState(observation: MatchObservation, humanPlayer: ReadOnlyPlayer, showBotInfo: Boolean = false) {
        clearConsole()
        
        printRoundInfo(observation)
        println()
        
        printScores(observation.players)
        println()
        
        printNukiCounts(observation)
        println()
        
        printHumanHand(humanPlayer)
        println()
        
        if (showBotInfo) {
            printBotHands(observation, humanPlayer)
            println()
            
            printBotAvailableActions(observation, humanPlayer)
            println()
        }
        
        printDiscards(observation)
        println()
        
        printSticks(observation)
        println()
        
        printWall(observation)
        
        val currentPlayer = observation.players.find { it.seat == observation.currentSeatWind }
        println("Current turn: ${currentPlayer?.let { getPlayerType(it, humanPlayer) } ?: "Unknown"} (${observation.currentSeatWind})")
    }
    
    fun printAvailableActions(actions: List<Action>, isDiscardTurn: Boolean) {
        println("Available actions:")
        actions.forEachIndexed { index, action ->
            val actionName = when (action) {
                is DiscardAction -> "Discard (choose tile)"
                else -> action.toString()
            }
            println("  [$index] $actionName")
        }
        println()
    }
    
    fun printTileSelection(hand: List<Tile>) {
        println("Select a tile to discard:")
        val sortedHand = hand.sortedWith(compareBy({ it.tileType::class.simpleName }, { it.value }))
        sortedHand.forEachIndexed { index, tile ->
            println("  [$index] ${tile.toHumanString()}")
        }
        println()
    }
    
    fun printRoundEndSummary(
        winner: ReadOnlyPlayer?,
        isTsumo: Boolean,
        isExhaustive: Boolean,
        payments: Map<Wind, Int>,
        observation: MatchObservation
    ) {
        println()
        println("===========================================")
        println("ROUND END")
        println("===========================================")
        
        if (isExhaustive && winner == null) {
            println("Result: Exhaustive Draw (Ryūkyoku)")
            printTenpaiStatus(observation)
        } else if (winner != null) {
            val winMethod = if (isTsumo) "Tsumo" else "Ron"
            println("Winner: ${winner.seat} ($winMethod)")
            
            payments.forEach { (wind, points) ->
                if (wind != winner.seat) {
                    println("  $wind pays: $points points")
                }
            }
        }
        
        println()
        println("Updated scores:")
        printScores(observation.players)
        println("===========================================")
        println()
    }
    
    fun printMatchEnd(players: List<ReadOnlyPlayer>) {
        println()
        println("+=========================================+")
        println("|         MATCH ENDED                     |")
        println("+=========================================+")
        println()
        println("Final standings:")
        val sortedPlayers = players.sortedByDescending { it.score }
        sortedPlayers.forEachIndexed { index, player ->
            val rank = index + 1
            println("  $rank. ${player.seat}: ${player.score} points")
        }
        println()
    }
    
    fun printError(message: String) {
        println()
        println("[ERROR] $message")
        println()
    }
    
    fun printInfo(message: String) {
        println(message)
    }
    
    private fun printRoundInfo(observation: MatchObservation) {
        val round = observation.roundRotationStatus.round
        val honba = observation.roundRotationStatus.honba
        val dealerWind = observation.roundRotationStatus.place
        
        println("===========================================")
        println("Round: $dealerWind $round (Honba: $honba)")
        println("===========================================")
    }
    
    private fun printScores(players: List<ReadOnlyPlayer>) {
        println("Scores:")
        players.forEach { player ->
            println("  ${player.seat}: ${player.score} points")
        }
    }
    
    private fun printHumanHand(player: ReadOnlyPlayer) {
        println("Your hand (${player.closeHand.size} tiles):")
        val sortedHand = player.closeHand.sortedWith(compareBy({ it.tileType::class.simpleName }, { it.value }))
        println("  ${sortedHand.joinToString(" ") { it.toHumanString() }}")
    }
    
    private fun printBotHands(observation: MatchObservation, humanPlayer: ReadOnlyPlayer) {
        println("Bot hands:")
        observation.players.forEach { player ->
            if (player != humanPlayer) {
                val sortedHand = player.closeHand.sortedWith(compareBy({ it.tileType::class.simpleName }, { it.value }))
                println("  ${player.seat} (${sortedHand.size} tiles): ${sortedHand.joinToString(" ") { it.toHumanString() }}")
            }
        }
    }
    
    private fun printBotAvailableActions(observation: MatchObservation, humanPlayer: ReadOnlyPlayer) {
        println("Bot available actions:")
        observation.players.forEach { player ->
            if (player != humanPlayer) {
                val isCurrentTurn = player.seat == observation.currentSeatWind
                if (isCurrentTurn) {
                    val actions = observation.availableActions
                    val actionNames = actions.map { action ->
                        when (action) {
                            is DiscardAction -> "Discard"
                            else -> action.toString()
                        }
                    }
                    println("  ${player.seat}: ${actionNames.joinToString(", ")}")
                } else {
                    println("  ${player.seat}: (waiting)")
                }
            }
        }
    }
    
    private fun printWall(observation: MatchObservation) {
        println("Wall (${observation.wall.size} tiles remaining):")
        val tileGroups = observation.wall.tileWall.groupBy { it.toHumanString() }
            .mapValues { it.value.size }
            .toList()
            .sortedBy { (tileName, _) -> tileName }
        
        val wallStr = tileGroups.joinToString(", ") { (tileName, count) -> "${count}x${tileName}" }
        println("  $wallStr")
        println()
        
        // Print dead wall structure
        printDeadWall(observation)
    }
    
    private fun printDeadWall(observation: MatchObservation) {
        println("Dead Wall (7 stacks x 2 tiles):")
        println("  +---------------------------------------------------------+")
        println("  | Stack:  0     1     2     3     4     5     6          |")
        println("  |        +-----+-----+-----+-----+-----+-----+-----+    |")
        println("  | Upper  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |    |")
        println("  |        +-----+-----+-----+-----+-----+-----+-----+    |")
        println("  | Lower  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |    |")
        println("  |        +-----+-----+-----+-----+-----+-----+-----+    |")
        println("  +---------------------------------------------------------+")
        println("  Legend: Stack 2 = Initial dora position")
        println("          Kan -> reveals upper tile on next stack")
        println("          Riichi -> reveals lower tile on next stack")
        println()
        
        // Show revealed dora indicators
        println("Revealed Dora Indicators:")
        if (observation.doraIndicators.isEmpty()) {
            println("  (none revealed yet)")
        } else {
            observation.doraIndicators.forEachIndexed { index, tile ->
                val position = when {
                    index == 0 -> "Initial (Stack 2 upper)"
                    else -> "Revealed #$index"
                }
                println("  [$index] ${tile.toHumanString()} - $position")
            }
        }
    }
    
    private fun printDoraIndicators(observation: MatchObservation) {
        // This is now handled by printDeadWall, but kept for compatibility
        // println("Dora indicators: ${observation.doraIndicators.joinToString(" ") { it.toHumanString() }}")
    }
    
    private fun printDiscards(observation: MatchObservation) {
        println("Discards:")
        observation.discards.forEach { (wind, tiles) ->
            val tileStr = tiles.joinToString(" ") { it.toHumanString() }
            println("  $wind: $tileStr")
        }
    }
    
    private fun printSticks(observation: MatchObservation) {
        println("Sticks: Riichi=${observation.riichiSticks}, Honba=${observation.honbaSticks}")
    }
    
    private fun printNukiCounts(observation: MatchObservation) {
        val nukiInfo = observation.players.mapNotNull { player ->
            if (player.nukiCount > 0) "${player.seat}: ${player.nukiCount} nuki" else null
        }
        if (nukiInfo.isNotEmpty()) {
            println("Nuki: ${nukiInfo.joinToString(", ")}")
        }
    }
    
    private fun printTenpaiStatus(observation: MatchObservation) {
        println("Tenpai status:")
        observation.players.forEach { player ->
            val isTenpai = false
            println("  ${player.seat}: ${if (isTenpai) "Tenpai" else "Noten"}")
        }
    }
    
    private fun getPlayerType(player: ReadOnlyPlayer, humanPlayer: ReadOnlyPlayer): String {
        return if (player == humanPlayer) "Human" else "Bot"
    }
    
    private fun clearConsole() {
        print("\u001B[2J\u001B[H")
        System.out.flush()
    }
}

fun Tile.toHumanString(): String {
    return when (tileType) {
        is xyz.uthofficial.arnyan.env.tile.Man -> "${value}m"
        is xyz.uthofficial.arnyan.env.tile.Pin -> "${value}p"
        is xyz.uthofficial.arnyan.env.tile.Sou -> "${value}s"
        is xyz.uthofficial.arnyan.env.tile.Wind -> when (value) {
            1 -> "E"
            2 -> "S"
            3 -> "W"
            4 -> "N"
            else -> "Wind$value"
        }
        is xyz.uthofficial.arnyan.env.tile.Dragon -> when (value) {
            1 -> "White"
            2 -> "Green"
            3 -> "Red"
            else -> "Dragon$value"
        }
        else -> "$tileType$value"
    } + if (isAka) " (aka)" else ""
}
