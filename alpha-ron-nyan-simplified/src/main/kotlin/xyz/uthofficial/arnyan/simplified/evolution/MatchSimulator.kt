package xyz.uthofficial.arnyan.simplified.evolution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import xyz.uthofficial.arnyan.demo.RandomBotPlayer
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.Match
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.StepResult
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.simplified.agent.EvolutionaryAgent
import xyz.uthofficial.arnyan.simplified.agent.Playstyle

class MatchSimulator {

    suspend fun evaluateAgent(
        agent: EvolutionaryAgent,
        opponents: List<EvolutionaryAgent>,
        games: Int,
        fitnessCalculator: FitnessCalculator
    ): Float = coroutineScope {
        val results = List(games) { gameIdx ->
            async(Dispatchers.IO.limitedParallelism(4)) {
                val match = createMatch(agent, opponents)
                val events = playMatch(match, agent)
                fitnessCalculator.calculate(agent, events)
            }
        }
        results.awaitAll().average().toFloat()
    }

    suspend fun evaluateAgentWithDetails(
        agent: EvolutionaryAgent,
        opponents: List<EvolutionaryAgent>,
        games: Int,
        fitnessCalculator: FitnessCalculator
    ): Pair<Float, List<List<MatchEvent>>> = coroutineScope {
        val results = List(games) { gameIdx ->
            async(Dispatchers.IO.limitedParallelism(4)) {
                val match = createMatch(agent, opponents)
                val events = playMatch(match, agent)
                val fitness = fitnessCalculator.calculate(agent, events)
                fitness to events
            }
        }
        val completed = results.awaitAll()
        val avgFitness = completed.map { it.first }.average().toFloat()
        val allEvents = completed.map { it.second }
        avgFitness to allEvents
    }

    fun createMatch(
        agent: EvolutionaryAgent,
        opponents: List<EvolutionaryAgent>
    ): Match {
        val agentCopy = agent.copyForMatch()
        val opponentCopies = opponents.map { it.copyForMatch() }

        val allPlayers = listOf(agentCopy) + opponentCopies

        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = allPlayers,
            shuffleWinds = true
        )

        return (matchResult as Result.Success).value
    }

    suspend fun playMatch(match: Match, agent: EvolutionaryAgent): List<MatchEvent> {
        val events = mutableListOf<MatchEvent>()
        val eventTracker = MatchEventTracker()

        val startResult = match.start()
        if (startResult is Result.Failure) {
            return events
        }

        val observation = match.observation
        val agentPlayerInMatch = observation.players.find { it.id == agent.id }
        if (agentPlayerInMatch != null && agentPlayerInMatch is EvolutionaryAgent) {
            agent.seat = agentPlayerInMatch.seat
        }

        var turnCount = 0
        val maxTurns = 180

        while (turnCount < maxTurns) {
            val observation = match.observation

            if (match.isRoundOver(observation.lastAction)) {
                break
            }

            val currentPlayer = observation.players.find {
                it.seat == observation.currentSeatWind
            } ?: break

            val isAgentPlayer = currentPlayer.id == agent.id
            val availableActions = observation.availableActions
            if (availableActions.isEmpty()) {
                turnCount++
                eventTracker.incrementTurn()
                continue
            }

            val player = currentPlayer as Player
            
            val (action, tile) = if (isAgentPlayer) {
                agent.selectAction(observation)
            } else {
                val (randomAction, needsTile) = RandomBotPlayer.selectAction(observation, player)
                val tileForRandom = if (needsTile) {
                    RandomBotPlayer.selectDiscardTile(player)
                } else {
                    null
                }
                randomAction to tileForRandom
            }

            if (tile == null && action !is PassAction) {
                turnCount++
                eventTracker.incrementTurn()
                continue
            }

            val playerHand = player.closeHand
            val tileToUse: Tile = tile ?: playerHand.firstOrNull() ?: run {
                turnCount++
                eventTracker.incrementTurn()
                continue
            }
            
            val result = match.submitAction(player, action, tileToUse)

            when (result) {
                is Result.Success -> {
                    recordEvent(eventTracker, action, observation, currentPlayer, agent)

                    if (result.value.isOver) {
                        recordRoundEndEvents(eventTracker, result.value, observation, agent)
                        break
                    }

                    turnCount++
                    eventTracker.incrementTurn()
                }
                is Result.Failure -> {
                    turnCount++
                    eventTracker.incrementTurn()
                }
            }
        }

        return eventTracker.getEvents()
    }

    private fun recordEvent(
        tracker: MatchEventTracker,
        action: Action,
        observation: MatchObservation,
        currentPlayer: xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer,
        agent: EvolutionaryAgent
    ) {
        val turn = observation.turnCount

        when (action.id) {
            Action.ID_RON -> {
                tracker.record(MatchEvent.RonWin(0))
            }
            Action.ID_TSUMO -> {
                tracker.record(MatchEvent.TsumoWin(0))
            }
            Action.ID_RIICHI -> {
                val isSuccessful = currentPlayer.id == agent.id && !agent.isRiichiDeclared
                tracker.record(MatchEvent.RiichiDeclared(turn, isSuccessful))
            }
            Action.ID_PON -> {
                tracker.record(MatchEvent.Pon(turn))
            }
            Action.ID_CHII -> {
                tracker.record(MatchEvent.Chii(turn))
            }
            Action.ID_ANKAN, Action.ID_MINKAN, Action.ID_KAKAN -> {
                val isAnkan = action.id == Action.ID_ANKAN
                tracker.record(MatchEvent.Kan(turn, isAnkan))
            }
            Action.ID_NUKI -> {
                tracker.record(MatchEvent.Kan(turn, true))
            }
            Action.ID_PASS -> {
                tracker.record(MatchEvent.PassAvailable(turn))
            }
            Action.ID_DISCARD -> {
            }
        }

        if (currentPlayer.id == agent.id && observation.furitenPlayers.contains(currentPlayer.seat)) {
            tracker.record(MatchEvent.Furiten)
        }
    }

    private fun recordRoundEndEvents(
        tracker: MatchEventTracker,
        stepResult: StepResult,
        observation: MatchObservation,
        agent: EvolutionaryAgent
    ) {
        val lastAction = observation.lastAction

        when (lastAction) {
            is LastAction.Ron -> {
                if (lastAction.player.id != agent.id) {
                    tracker.record(MatchEvent.DealIn(0))
                }
            }
            is LastAction.TsuMo -> {
            }
            else -> {
                if (observation.wall.size == 0) {
                    val isAgentTenpai = observation.furitenPlayers.none { it == agent.seat }
                    if (isAgentTenpai) {
                        tracker.record(MatchEvent.TenpaiEnd(agent.isRiichiDeclared))
                    }
                }
            }
        }
    }

    fun createRandomOpponents(count: Int): List<EvolutionaryAgent> {
        return List(count) {
            val network = xyz.uthofficial.arnyan.simplified.network.SanmaPolicyNetwork()
            val randomWeights = FloatArray(network.countParameters()) {
                kotlin.random.Random.nextFloat() * 0.1f - 0.05f
            }
            network.setWeights(randomWeights)

            EvolutionaryAgent(
                network = network,
                playstyle = if (kotlin.random.Random.nextBoolean()) Playstyle.AGGRESSIVE else Playstyle.CONSERVATIVE,
                temperature = 1.0f
            )
        }
    }

}
