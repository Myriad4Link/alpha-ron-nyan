package xyz.uthofficial.arnyan.demo

import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.Match
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardRoundWindCycle

class GameLoop {
    companion object {
        private const val INITIAL_SCORE = 25000
        private const val EAST_ROUNDS = 4
        private const val SOUTH_ROUNDS = 4
        
        fun run() {
            val playerName = UserInput.readPlayerName()
            val humanPlayer = HumanPlayer(name = playerName, score = INITIAL_SCORE)
            val bot1 = BotPlayer(score = INITIAL_SCORE)
            val bot2 = BotPlayer(score = INITIAL_SCORE)
            
            val players = listOf(humanPlayer, bot1, bot2)
            
            ConsoleDisplay.printInfo("Welcome to Sanma Mahjong Demo!")
            ConsoleDisplay.printInfo("Playing as: $playerName")
            ConsoleDisplay.printInfo("Starting scores: $INITIAL_SCORE points each")
            ConsoleDisplay.printInfo("")
            
            var currentRound = 1
            var currentDealerIndex = 0
            var continueMatch = true
            
            while (continueMatch && currentRound <= EAST_ROUNDS + SOUTH_ROUNDS) {
                val isEastRound = currentRound <= EAST_ROUNDS
                val roundWind = if (isEastRound) StandardWind.EAST else StandardWind.SOUTH
                val roundNumber = if (isEastRound) currentRound else currentRound - EAST_ROUNDS
                
                val ruleSet = createSanmaRuleSet(roundWind, roundNumber)
                val listeners = listOf(DemoMatchListener())
                
                val matchResult = Match.create(
                    ruleSet = ruleSet,
                    listeners = listeners,
                    playerList = players,
                    shuffleWinds = false
                )
                
                if (matchResult is Result.Failure) {
                    ConsoleDisplay.printError("Failed to create match: ${matchResult.error}")
                    break
                }
                
                val match = matchResult as Result.Success<Match>
                
                assignSeats(players, currentDealerIndex)
                
                val startResult = match.value.start()
                if (startResult is Result.Failure) {
                    ConsoleDisplay.printError("Failed to start match: ${startResult.error}")
                    break
                }
                
                listeners.forEach { it.onRoundStarted(match.value.observation) }
                
                val roundResult = playRound(match.value, humanPlayer)
                
                if (roundResult.shouldContinue) {
                    currentDealerIndex = roundResult.nextDealerIndex
                    currentRound = roundResult.nextRound
                    continueMatch = true
                } else {
                    continueMatch = false
                }
            }
            
            ConsoleDisplay.printMatchEnd(players.map { it as ReadOnlyPlayer })
        }
        
        private fun createSanmaRuleSet(roundWind: StandardWind, roundNumber: Int): RuleSet {
            return RuleSet.RIICHI_SANMA_ARI_ARI.copy(
                roundWindRotationRule = {
                    val cycle = StandardRoundWindCycle.fromMap(mapOf(roundWind to roundNumber)).getOrThrow()
                    Result.Success(cycle)
                }
            )
        }
        
        private fun assignSeats(players: List<Player>, dealerIndex: Int) {
            val winds = listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)
            players.forEachIndexed { index, player ->
                val windIndex = (dealerIndex + index) % winds.size
                player.seat = winds[windIndex]
            }
        }
        
        private fun playRound(match: Match, humanPlayer: HumanPlayer): RoundResult {
            var turnCount = 0
            val maxTurns = 200
            
            while (turnCount < maxTurns) {
                val observation = match.observation
                val currentPlayer = observation.players.find { it.seat == observation.currentSeatWind }
                    ?: break
                
                val isHumanTurn = currentPlayer == humanPlayer
                
                ConsoleDisplay.printGameState(observation, humanPlayer, showBotInfo = true)
                
                val availableActions = observation.availableActions
                if (availableActions.isEmpty()) {
                    ConsoleDisplay.printInfo("No available actions, passing...")
                    Thread.sleep(1000)
                    turnCount++
                    continue
                }
                
                val (action, needsTileSelection) = if (isHumanTurn) {
                    ConsoleDisplay.printAvailableActions(availableActions, availableActions.contains(DiscardAction))
                    val actionIndex = UserInput.readActionIndex(availableActions.size - 1)
                    val selectedAction = availableActions[actionIndex]
                    selectedAction to (selectedAction == DiscardAction)
                } else {
                    val (botAction, needsDiscard) = RandomBotPlayer.selectAction(observation, currentPlayer as Player)
                    botAction to needsDiscard
                }
                
                val tile = if (needsTileSelection) {
                    if (isHumanTurn) {
                        ConsoleDisplay.printTileSelection(humanPlayer.closeHand)
                        val tileIndex = UserInput.readTileIndex(humanPlayer.closeHand.size)
                        val sortedHand = humanPlayer.closeHand.sortedWith(
                            compareBy({ it.tileType::class.simpleName }, { it.value })
                        )
                        sortedHand[tileIndex]
                    } else {
                        RandomBotPlayer.selectDiscardTile(currentPlayer as Player)
                    }
                } else {
                    when (val lastAction = observation.lastAction) {
                        is LastAction.Discard -> lastAction.tile
                        is LastAction.Draw -> lastAction.tile
                        else -> {
                            ConsoleDisplay.printError("No tile available for action")
                            continue
                        }
                    }
                }

                when (val result = match.submitAction(currentPlayer as Player, action, tile)) {
                    is Result.Success -> {
                        ConsoleDisplay.printInfo("→ ${currentPlayer.seat} (${if (isHumanTurn) "Human" else "Bot"}) performed $action with ${tile.toHumanString()}")
                        
                        if (result.value.isOver) {
                            handleRoundEnd(match, result.value, currentPlayer, action)
                            return determineNextRound(match)
                        }
                        
                        turnCount++
                    }
                    is Result.Failure -> {
                        ConsoleDisplay.printError("Action failed: ${result.error}")
                        Thread.sleep(1500)
                    }
                }
            }
            
            ConsoleDisplay.printInfo("Round ended due to maximum turns reached")
            return RoundResult(shouldContinue = false, nextDealerIndex = 0, nextRound = 0)
        }
        
        private fun handleRoundEnd(
            match: Match,
            stepResult: xyz.uthofficial.arnyan.env.match.StepResult,
            winner: Player,
            winningAction: Action
        ) {
            val isTsumo = winningAction == xyz.uthofficial.arnyan.env.match.actions.TsuMo
            val isExhaustive = match.observation.wall.size == 0

            stepResult.stateChanges
                .filterIsInstance<xyz.uthofficial.arnyan.env.match.StateChange.UpdatePlayerScore>()
                .associate { it.seat to it.delta }
            
            ConsoleDisplay.printRoundEndSummary(
                winner = if (!isExhaustive) winner else null,
                isTsumo = isTsumo,
                isExhaustive = isExhaustive,
                payments = emptyMap(),
                observation = match.observation
            )
        }
        
        private fun determineNextRound(match: Match): RoundResult {
            val currentDealer = match.observation.roundRotationStatus.place
            val currentRoundNum = match.observation.roundRotationStatus.round
            val isEastRound = currentDealer == StandardWind.EAST
            
            val continuePlaying = UserInput.readContinuePrompt()
            if (!continuePlaying) {
                return RoundResult(shouldContinue = false, nextDealerIndex = 0, nextRound = 0)
            }
            
            val winner = when (val lastAction = match.observation.lastAction) {
                is LastAction.Ron -> lastAction.player
                is LastAction.TsuMo -> lastAction.player
                else -> null
            }
            
            val dealerWasWinner = winner?.seat == currentDealer
            val isExhaustive = match.observation.wall.size == 0
            
            val nextDealerIndex = if (dealerWasWinner || isExhaustive) {
                val currentPlayerIndex = match.observation.players.indexOfFirst { it.seat == currentDealer }
                currentPlayerIndex
            } else {
                val currentPlayerIndex = match.observation.players.indexOfFirst { it.seat == currentDealer }
                (currentPlayerIndex + 1) % 3
            }
            
            val nextRound = if (!dealerWasWinner && !isExhaustive) {
                currentRoundNum + 1
            } else {
                currentRoundNum
            }

            if (isEastRound) EAST_ROUNDS else EAST_ROUNDS + SOUTH_ROUNDS
            if (nextRound > (if (isEastRound) EAST_ROUNDS else SOUTH_ROUNDS)) {
                if (isEastRound) {
                    ConsoleDisplay.printInfo("Moving to South rounds...")
                } else {
                    ConsoleDisplay.printInfo("All rounds completed!")
                    return RoundResult(shouldContinue = false, nextDealerIndex = 0, nextRound = 0)
                }
            }
            
            return RoundResult(
                shouldContinue = true,
                nextDealerIndex = nextDealerIndex,
                nextRound = if (isEastRound) nextRound else EAST_ROUNDS + nextRound
            )
        }
    }
    
    data class RoundResult(
        val shouldContinue: Boolean,
        val nextDealerIndex: Int,
        val nextRound: Int
    )
}
