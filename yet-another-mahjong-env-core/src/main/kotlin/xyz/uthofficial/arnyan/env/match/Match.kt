package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction

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

    fun checkOver(): Boolean = engine.checkOver(state)

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
