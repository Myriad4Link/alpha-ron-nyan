package xyz.uthofficial.arnyan.simplified

import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.index.NDIndex
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.DoraCalculator.index

/**
 * CNN Observation Encoder for Sanma (3-player Mahjong).
 * 
 * Encodes MatchObservation into a tensor suitable for CNN processing.
 * 
 * Output shape: [11, 27]
 * - 11 channels (feature planes)
 * - 27 tile types (sanma tile set)
 * 
 * Channel breakdown:
 * 0. Closed hand count (normalized: count/4, range 0-1)
 * 1. Open hand count (normalized: count/4, range 0-1)
 * 2. My discards (normalized: count/4, range 0-1)
 * 3. Visible tiles ratio ((opened + discarded + dora) / 4.0, range 0-1)
 * 4. Dora value (binary: 1.0 if dora, 0.0 otherwise; includes aka 5p/5s)
 * 5. Round wind (one-hot: E=0, S=1, W=2)
 * 6. Seat wind (one-hot: E=0, S=1, W=2)
 * 7. Riichi state (which seats declared riichi: E=0, S=1, W=2)
 * 8. Turn counter (turnCount / 72.0, range 0-1)
 * 9. Score difference (0.5 = tied, 1.0 = ahead, 0.0 = behind)
 * 10. Available actions (binary mask for 11 action types, indices 11-26 zero-padded)
 */
class SanmaObservationEncoder {
    
    companion object {
        const val CHANNEL_COUNT = 11
        const val TILE_COUNT = 27
        }
    
    /**
     * Encodes the match observation into a tensor for CNN processing.
     * 
     * @param manager NDManager for creating NDArray
     * @param observation Current match observation
     * @param player The player perspective to encode from
     * @return NDArray of shape [11, 27]
     */
    fun encode(
        manager: NDManager,
        observation: MatchObservation,
        player: Player
    ): NDArray {
        val tensor = manager.zeros(Shape(CHANNEL_COUNT.toLong(), TILE_COUNT.toLong()), DataType.FLOAT32)
        
        // Build tile histograms
        val closedHandHist = buildTileHistogram(player.closeHand)
        val openHandHist = buildOpenHandHistogram(player.openHand)
        val myDiscardsHist = buildTileHistogram(observation.discards[player.seat] ?: emptyList())
        
        // Channel 0: Closed hand count (normalized by 4)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("0, $i"), closedHandHist[i] / 4.0f)
        }
        
        // Channel 1: Open hand count (normalized by 4)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("1, $i"), openHandHist[i] / 4.0f)
        }
        
        // Channel 2: My discards (normalized by 4)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("2, $i"), myDiscardsHist[i] / 4.0f)
        }
        
        // Channel 3: Visible tiles ratio
        val visibleHist = calculateVisibleTiles(observation, player)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("3, $i"), visibleHist[i])
        }
        
        // Channel 4: Dora value (binary: 1 if dora, 0 otherwise)
        val doraValues = DoraValueCalculator.calculateDoraValues(
            player.closeHand + player.openHand.flatten(),
            observation.doraIndicators
        )
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("4, $i"), doraValues[i])
        }
        
        // Channel 5: Round wind (one-hot: E=0, S=1, W=2)
        encodeWind(tensor, 5, observation.roundRotationStatus.place)
        
        // Channel 6: Seat wind (one-hot: E=0, S=1, W=2)
        player.seat?.let { encodeWind(tensor, 6, it) }
        
        // Channel 7: Riichi state (which seats declared riichi)
        encodeRiichiState(tensor, 7, observation, player)
        
        // Channel 8: Turn counter (normalized to [0-1])
        val turnNormalized = (observation.turnCount / 72.0f).coerceIn(0f, 1f)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("8, $i"), turnNormalized)
        }
        
        // Channel 9: Score difference (normalized to [0-1])
        val scoreDiff = calculateScoreDifference(observation, player)
        for (i in 0 until TILE_COUNT) {
            tensor.set(NDIndex("9, $i"), scoreDiff)
        }
        
        // Channel 10: Available actions (binary mask)
        encodeAvailableActions(tensor, 10, observation)
        
        return tensor
    }
    
    /**
     * Builds a tile histogram for a list of tiles.
     * @return IntArray of size 27 with counts for each tile type
     */
    private fun buildTileHistogram(tiles: List<Tile>): IntArray {
        val hist = IntArray(TILE_COUNT)
        for (tile in tiles) {
            val registryIndex = tile.index()
            val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
            if (sanmaIndex != null) {
                hist[sanmaIndex]++
            }
        }
        return hist
    }
    
    /**
     * Builds histogram for open hand (exposed mentsus).
     */
    private fun buildOpenHandHistogram(openHand: List<List<Tile>>): IntArray {
        val hist = IntArray(TILE_COUNT)
        for (mentsu in openHand) {
            for (tile in mentsu) {
                val registryIndex = tile.index()
                val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
                if (sanmaIndex != null) {
                    hist[sanmaIndex]++
                }
            }
        }
        return hist
    }
    
    /**
     * Calculates visible tiles ratio for each tile type.
     * Visible = opened by all players + all discards + dora indicators
     */
    private fun calculateVisibleTiles(observation: MatchObservation, perspectivePlayer: Player): FloatArray {
        val visibleHist = FloatArray(TILE_COUNT) { 0f }
        
        // Count opened tiles from all players
        for (player in observation.players) {
            for (mentsu in player.openHand) {
                for (tile in mentsu) {
                    val registryIndex = tile.index()
                    val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
                    if (sanmaIndex != null) {
                        visibleHist[sanmaIndex] += 1f
                    }
                }
            }
        }
        
        // Count all discards
        for ((_, discards) in observation.discards) {
            for (tile in discards) {
                val registryIndex = tile.index()
                val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
                if (sanmaIndex != null) {
                    visibleHist[sanmaIndex] += 1f
                }
            }
        }
        
        // Count dora indicators
        for (tile in observation.doraIndicators) {
            val registryIndex = tile.index()
            val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
            if (sanmaIndex != null) {
                visibleHist[sanmaIndex] += 1f
            }
        }
        
        // Normalize by 4 (max copies of each tile)
        for (i in 0 until TILE_COUNT) {
            visibleHist[i] = (visibleHist[i] / 4.0f).coerceIn(0f, 1f)
        }
        
        return visibleHist
    }
    
    /**
     * Encodes wind as one-hot in first 3 indices (E, S, W).
     * North is not encoded in sanma (no North seat).
     */
    private fun encodeWind(tensor: NDArray, channel: Int, wind: Wind) {
        val windIndex = when (wind) {
            StandardWind.EAST -> 0
            StandardWind.SOUTH -> 1
            StandardWind.WEST -> 2
            StandardWind.NORTH -> 0  // North not used in sanma seating
            else -> 0
        }
        tensor.set(NDIndex("$channel, $windIndex"), 1f)
    }
    
    /**
     * Encodes riichi state for all seats (E, S, W).
     */
    private fun encodeRiichiState(
        tensor: NDArray,
        channel: Int,
        observation: MatchObservation,
        perspectivePlayer: Player
    ) {
        for (player in observation.players) {
            if (player is Player && player.isRiichiDeclared) {
                player.seat?.let { seat ->
                    val windIndex = when (seat) {
                        StandardWind.EAST -> 0
                        StandardWind.SOUTH -> 1
                        StandardWind.WEST -> 2
                        else -> return
                    }
                    tensor.set(NDIndex("$channel, $windIndex"), 1f)
                }
            }
        }
    }
    
    /**
     * Calculates score difference from perspective player's viewpoint.
     * Normalized to [0-1] where 0.5 = tied, 1.0 = far ahead, 0.0 = far behind.
     */
    private fun calculateScoreDifference(observation: MatchObservation, player: Player): Float {
        val myScore = player.score
        val opponentScores = observation.players
            .filter { it != player }
            .map { it.score }
        
        val avgOpponentScore = opponentScores.average().toFloat()
        val diff = myScore - avgOpponentScore
        
        return ((diff / 50000f) + 0.5f).coerceIn(0f, 1f)
    }
    
    /**
     * Encodes available actions as binary mask.
     * First 11 indices represent action types, indices 11-26 are zero-padded.
     */
    private fun encodeAvailableActions(tensor: NDArray, channel: Int, observation: MatchObservation) {
        val availableActions = observation.availableActions
        
        for (action in availableActions) {
            val actionIndex = when (action.id) {
                Action.ID_CHII -> 0
                Action.ID_PON -> 1
                Action.ID_RON -> 2
                Action.ID_TSUMO -> 3
                Action.ID_DISCARD -> 4
                Action.ID_PASS -> 5
                Action.ID_RIICHI -> 6
                Action.ID_ANKAN -> 7
                Action.ID_MINKAN -> 8
                Action.ID_KAKAN -> 9
                Action.ID_NUKI -> 10
                else -> continue
            }
            tensor.set(NDIndex("$channel, $actionIndex"), 1f)
        }
        // Indices 11-26 remain zero (padded)
    }
}
