package xyz.uthofficial.arnyan.simplified

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.yaku.DoraCalculator

/**
 * Calculates dora values for Sanma observation encoding.
 * 
 * Channel 4 encoding:
 * - Binary 1/0 for each tile type indicating if it's a dora tile
 * - 5p and 5s are initially 1 (aka dora)
 * - Tiles matching revealed dora indicators are marked as 1
 * - Updates only when dora indicators change (through kan/riichi)
 */
object DoraValueCalculator {
    
    /**
     * Calculates binary dora indicators for all 27 sanma tile types.
     * 
     * @param hand Player's hand (closed + open)
     * @param doraIndicators Currently revealed dora indicators from dead wall
     * @return FloatArray of size 27 with 1.0f for dora tiles, 0.0f otherwise
     */
    fun calculateDoraValues(
        hand: List<Tile>,
        doraIndicators: List<Tile>
    ): FloatArray {
        val doraValues = FloatArray(SanmaTileMapping.SANMA_SIZE) { 0f }
        
        // Mark aka dora (5p and 5s) as 1
        // Sanma index 9 = 5p, Sanma index 18 = 5s
        doraValues[9] = 1f   // 5p aka
        doraValues[18] = 1f  // 5s aka
        
        // Mark tiles that match dora indicators
        val doraTiles = DoraCalculator.getDoraTiles(doraIndicators)
        
        for (doraTile in doraTiles) {
            val registryIndex = doraTile.tileType.intRange.first + doraTile.value - 1
            val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
            if (sanmaIndex != null) {
                doraValues[sanmaIndex] = 1f
            }
        }
        
        return doraValues
    }
    
    /**
     * Checks if a specific tile is a dora tile.
     * 
     * @param tile The tile to check
     * @param doraIndicators Currently revealed dora indicators
     * @return true if the tile is a dora (including aka)
     */
    fun isDoraTile(tile: Tile, doraIndicators: List<Tile>): Boolean {
        // Check if aka
        if (tile.isAka) return true
        
        // Check if matches dora indicator
        val doraTiles = DoraCalculator.getDoraTiles(doraIndicators)
        return doraTiles.any { dora ->
            dora.tileType == tile.tileType && dora.value == tile.value
        }
    }
    
    /**
     * Counts total dora tiles in hand.
     * 
     * @param hand Player's hand
     * @param doraIndicators Currently revealed dora indicators
     * @return Total count of dora tiles (including aka)
     */
    fun countTotalDora(hand: List<Tile>, doraIndicators: List<Tile>): Int {
        var count = 0
        for (tile in hand) {
            if (isDoraTile(tile, doraIndicators)) {
                count++
            }
        }
        return count
    }
}
