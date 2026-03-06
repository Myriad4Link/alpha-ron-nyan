package xyz.uthofficial.arnyan.simplified.agent

import ai.djl.ndarray.NDManager
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.actions.AnKan
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.KaKan
import xyz.uthofficial.arnyan.env.match.actions.MinKan
import xyz.uthofficial.arnyan.env.match.actions.NukiPei
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import xyz.uthofficial.arnyan.simplified.SanmaObservationEncoder
import xyz.uthofficial.arnyan.simplified.network.SanmaPolicyNetwork
import java.util.UUID

class EvolutionaryAgent(
    val network: SanmaPolicyNetwork,
    val playstyle: Playstyle,
    private val temperature: Float = 1.0f,
    override val id: UUID = UUID.randomUUID(),
    override val closeHand: MutableList<Tile> = mutableListOf(),
    override val openHand: MutableList<List<Tile>> = mutableListOf(),
    override val currentMentsusComposition: MutableList<List<Mentsu>> = mutableListOf(),
    override var seat: Wind? = null,
    override var score: Int = 25000,
    override var isRiichiDeclared: Boolean = false,
    override var riichiSticksDeposited: Int = 0,
    override var nukiCount: Int = 0
) : Player {

    private val observationEncoder = SanmaObservationEncoder()
    private val actionMapping = mapOf(
        0 to Chii,
        1 to Pon,
        2 to Ron,
        3 to TsuMo,
        4 to DiscardAction,
        5 to PassAction,
        6 to RiichiAction,
        7 to AnKan,
        8 to MinKan,
        9 to KaKan,
        10 to NukiPei
    )

    fun selectAction(observation: MatchObservation): Pair<Action, Tile?> {
        NDManager.newBaseManager().use { manager ->
            val playerData = observation.players.find { it.seat == observation.currentSeatWind }
                ?: throw IllegalStateException("Current player not found in observation")

            val player = playerData as? xyz.uthofficial.arnyan.env.player.Player
                ?: throw IllegalStateException("Player data is not mutable Player")

            val encoded = observationEncoder.encode(manager, observation, player)
            val reshaped = encoded.reshape(1, 11, 27)

            val (subjectProbs, actionProbs) = network.forward(reshaped)

            val availableActions = observation.availableActions
            val actionMask = FloatArray(11) { i ->
                val action = actionMapping[i]
                if (action != null && action in availableActions) 1f else 0f
            }

            val maskedActionProbs = actionProbs.mul(manager.create(actionMask))
            val sum = maskedActionProbs.sum().toFloatArray()[0]
            val normalizedProbs = if (sum > 0) maskedActionProbs.div(sum) else maskedActionProbs

            val selectedActionIdx = sampleFromDistribution(normalizedProbs.toFloatArray())
            val selectedAction = actionMapping[selectedActionIdx]
                ?: throw IllegalStateException("Invalid action index: $selectedActionIdx")

            if (selectedAction !in availableActions) {
                return availableActions.filter { it != PassAction }.firstOrNull()?.let { it to null }
                    ?: (PassAction to null)
            }

            val needsSubjectTile = when (selectedAction.id) {
                Action.ID_DISCARD, Action.ID_RIICHI -> true
                Action.ID_CHII, Action.ID_PON, Action.ID_RON, Action.ID_MINKAN -> {
                    val lastAction = observation.lastAction as? LastAction.Discard
                    if (lastAction != null) return selectedAction to lastAction.tile
                    return selectedAction to null
                }
                Action.ID_TSUMO, Action.ID_ANKAN, Action.ID_KAKAN, Action.ID_NUKI -> {
                    val lastAction = observation.lastAction as? LastAction.Draw
                    if (lastAction != null) return selectedAction to lastAction.tile
                    return selectedAction to null
                }
                Action.ID_PASS -> return selectedAction to null
                else -> false
            }

            if (!needsSubjectTile) {
                return selectedAction to null
            }

            val subjectTile = selectSubjectTile(subjectProbs, player, selectedAction, observation)
            return selectedAction to subjectTile
        }
    }

    private fun selectSubjectTile(
        subjectProbs: ai.djl.ndarray.NDArray,
        player: Player,
        action: Action,
        observation: MatchObservation
    ): Tile? {
        val hand = player.closeHand
        if (hand.isEmpty()) return null

        val probs = subjectProbs.toFloatArray()

        val validTiles = when (action.id) {
            Action.ID_DISCARD, Action.ID_RIICHI -> hand
            else -> hand
        }

        if (validTiles.isEmpty()) return null

        val tileProbs = validTiles.map { tile ->
            val registryIndex = tile.index()
            val sanmaIndex = xyz.uthofficial.arnyan.simplified.SanmaTileMapping.registryToSanma(registryIndex)
            sanmaIndex?.let { probs.getOrElse(it) { 0f } } ?: 0f
        }.toFloatArray()

        val sum = tileProbs.sum()
        val normalized = if (sum > 0) tileProbs.map { it / sum }.toFloatArray() else tileProbs

        val selectedIdx = sampleFromDistribution(normalized)
        return validTiles.getOrElse(selectedIdx) { validTiles.first() }
    }

    private fun sampleFromDistribution(probs: FloatArray): Int {
        val r = kotlin.random.Random.nextFloat()
        var cumulative = 0f
        for (i in probs.indices) {
            cumulative += probs[i]
            if (r <= cumulative) return i
        }
        return probs.lastIndex
    }

    fun cloneWithWeights(newWeights: FloatArray): EvolutionaryAgent {
        val newNetwork = SanmaPolicyNetwork()
        try {
            newNetwork.setWeights(newWeights)
        } catch (e: Exception) {
            newNetwork.close()
            throw e
        }

        return EvolutionaryAgent(
            network = newNetwork,
            playstyle = playstyle,
            temperature = temperature,
            id = UUID.randomUUID(),
            closeHand = mutableListOf(),
            openHand = mutableListOf(),
            currentMentsusComposition = mutableListOf(),
            seat = null,
            score = score,
            isRiichiDeclared = false,
            riichiSticksDeposited = 0,
            nukiCount = 0
        )
    }

    fun copyForMatch(): EvolutionaryAgent {
        return EvolutionaryAgent(
            network = network,
            playstyle = playstyle,
            temperature = temperature,
            id = id,
            closeHand = mutableListOf(),
            openHand = mutableListOf(),
            currentMentsusComposition = mutableListOf(),
            seat = null,
            score = score,
            isRiichiDeclared = false,
            riichiSticksDeposited = 0,
            nukiCount = 0
        )
    }
}

private fun Tile.index(): Int = when (tileType) {
    is xyz.uthofficial.arnyan.env.tile.Dragon -> value - 1
    is xyz.uthofficial.arnyan.env.tile.Man -> value + 2
    is xyz.uthofficial.arnyan.env.tile.Pin -> value + 11
    is xyz.uthofficial.arnyan.env.tile.Sou -> value + 20
    is xyz.uthofficial.arnyan.env.tile.Wind -> value + 29
    else -> -1
}
