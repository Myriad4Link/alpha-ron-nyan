package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction

data class RoundInfo(
    val round: Int,
    val honba: Int,
    val dealerWind: Wind
)

class Match private constructor(
    private val listeners: List<MatchListener>,
    private val state: MatchState,
    private val engine: MatchEngine = MatchEngine()
) {
    fun start(): Result<StepResult, ActionError> = binding {
        val stepResult = engine.start(state).bind()
        listeners.forEach { it.onMatchStarted(observation) }
        stepResult
    }

    fun submitAction(player: Player, action: Action, subject: Tile): Result<StepResult, ActionError> = binding {
        val stepResult = engine.submitAction(state, player, action, subject).bind()
        stepResult
    }

    fun submitDiscard(player: Player, tile: Tile): Result<StepResult, ActionError> =
        submitAction(player, DiscardAction, tile)

    fun next(): Result<StepResult, ActionError> = binding {
        engine.start(state).bind()
    }

    fun checkOver(): Boolean = engine.checkOver(state)

    fun isRoundOver(lastAction: LastAction): Boolean {
        return lastAction is LastAction.Ron || lastAction is LastAction.TsuMo || state.wall.size == 0
    }

    fun endRound(winnerSeat: Wind? = null): Result<StepResult, ActionError> = binding {
        val isDealerWin = winnerSeat == state.roundRotationStatus.place
        val isExhaustive = state.wall.size == 0
        
        if (isExhaustive && winnerSeat == null) {
            state.roundRotationStatus = state.roundRotationStatus.copy(
                honba = state.roundRotationStatus.honba + 1
            )
            state.honbaSticks = state.roundRotationStatus.honba
            
            StepResult(state.toObservation(), state.currentSeatWind, true)
        } else {
            val shouldContinueDealer = isDealerWin || isExhaustive
            
            state.roundRotationStatus = if (shouldContinueDealer) {
                state.roundRotationStatus.copy(honba = state.roundRotationStatus.honba + 1)
            } else {
                state.roundRotationStatus.copy(honba = 0)
            }
            
            state.honbaSticks = state.roundRotationStatus.honba
            state.riichiSticks = 0
            
            if (!shouldContinueDealer) {
                val nextPlayerWind = state.topology.getShimocha(state.currentSeatWind).getOrThrow()
                state.currentSeatWind = nextPlayerWind
            }
            
            StepResult(state.toObservation(), state.currentSeatWind, false)
        }
    }

    fun startNextRound(): Result<StepResult, ActionError> = binding {
        state.discards.clear()
        state.topology.seats.forEach {
            state.discards[it] = mutableListOf()
        }
        state.lastAction = LastAction.None
        state.passedPlayers.clear()
        state.furitenPlayers.clear()
        state.temporaryFuritenPlayers.clear()
        state.availableActionsMaskPerPlayer.clear()
        state.topology.seats.forEach {
            state.availableActionsMaskPerPlayer[it] = 0
        }
        
        state.players.forEach { player ->
            player.closeHand.clear()
            player.openHand.clear()
            player.currentMentsusComposition.clear()
            player.isRiichiDeclared = false
            player.riichiSticksDeposited = 0
        }
        
        require(state.wall.size >= state.players.size * state.wall.standardDealAmount) {
            "Not enough tiles in wall: ${state.wall.size} < ${state.players.size * state.wall.standardDealAmount}"
        }
        
        (state.wall deal state.wall.standardDealAmount randomlyTo state.players)
            .mapError { ActionError.Wall(it) }
            .bind()
        
        engine.start(state).bind()
    }

    val isMatchOver: Boolean
        get() {
            val currentRound = state.roundRotationStatus.round
            val totalRounds = 1
            return currentRound >= totalRounds
        }

    val currentRoundInfo: RoundInfo
        get() = RoundInfo(
            round = state.roundRotationStatus.round,
            honba = state.roundRotationStatus.honba,
            dealerWind = state.roundRotationStatus.place
        )

    val observation: MatchObservation
        get() = state.toObservation().copy(
            availableActions = engine.maskToActions(
                state.availableActionsMaskPerPlayer[state.currentSeatWind] ?: 0
            )
        )

    companion object {
        fun create(
            ruleSet: RuleSet,
            listeners: List<MatchListener>,
            playerList: List<Player>,
            shuffleWinds: Boolean
        ) = binding {
            val wall = ruleSet.wallGenerationRule.build().bind()
            (wall deal wall.standardDealAmount randomlyTo playerList).bind()

            val topology = ruleSet.playerWindRotationOrderRule.build().bind()
            val roundWindCycle = ruleSet.roundWindRotationRule.build().bind()
            val yakuConfiguration = ruleSet.yakuRule.build()
            val scoringCalculator = ruleSet.scoringRule.build()

            if (playerList.size != topology.seats.size) {
                Result.Failure(
                    ConfigurationError.MatchConfigurationError.PlayerCountMismatch(
                        playerCount = playerList.size,
                        seatCount = topology.seats.size
                    )
                ).bind()
            }

            if (shuffleWinds)
                playerList.assignSeatRandomly(topology)
            else
                playerList.assignSeatInOrder(topology)

            val currentSeatWind = topology.firstSeatWind

            val state = MatchState(
                players = playerList,
                wall = wall,
                topology = topology,
                currentSeatWind = currentSeatWind,
                roundRotationStatus = roundWindCycle.startRoundRotationStatus,
                yakuConfiguration = yakuConfiguration,
                scoringCalculator = scoringCalculator
            )
            val match = Match(listeners, state)

            listeners.forEach { it.onMatchStarted(match.observation) }
            match
        }

        private fun List<Player>.assignSeatRandomly(topology: TableTopology) {
            val winds = topology.seats.shuffled()
            forEachIndexed { index, player ->
                player.seat = winds[index % winds.size]
            }
        }

        private fun List<Player>.assignSeatInOrder(topology: TableTopology) {
            val winds = topology.seats
            forEachIndexed { index, player ->
                player.seat = winds[index % winds.size]
            }
        }
    }
}
