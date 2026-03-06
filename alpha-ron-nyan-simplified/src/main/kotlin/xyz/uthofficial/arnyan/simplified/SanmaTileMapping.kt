package xyz.uthofficial.arnyan.simplified

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

/**
 * Tile index mapping for Sanma (3-player Mahjong).
 * 
 * Sanma uses 108 tiles (vs 136 in Four-player):
 * - All 1-9 Pinzu (9 types × 4 = 36 tiles)
 * - All 1-9 Souzu (9 types × 4 = 36 tiles)
 * - 1m and 9m only (2 types × 4 = 8 tiles)
 * - All Wind tiles (4 types × 4 = 16 tiles)
 * - All Dragon tiles (3 types × 4 = 12 tiles)
 * 
 * Excludes: 2m-8m (28 tiles removed)
 * 
 * Mapping from 34-tile registry to 27-tile sanma encoding:
 * Registry 0-2 (Dragon)   → Sanma 0-2
 * Registry 3 (1m)         → Sanma 3
 * Registry 11 (9m)        → Sanma 4
 * Registry 12-20 (Pin)    → Sanma 5-13
 * Registry 21-29 (Sou)    → Sanma 14-22
 * Registry 30-33 (Wind)   → Sanma 23-26
 */
object SanmaTileMapping {
    const val SANMA_SIZE = 27
    private const val REGISTRY_SIZE = 34
    
    /**
     * Maps registry tile indices to sanma indices.
     * Returns null for excluded tiles (2m-8m).
     */
    private val registryToSanmaMap: IntArray = intArrayOf(
        0,   // 0: 1 Dragon (White)
        1,   // 1: 2 Dragon (Green)
        2,   // 2: 3 Dragon (Red)
        3,   // 3: 1m
        -1,  // 4: 2m (excluded)
        -1,  // 5: 3m (excluded)
        -1,  // 6: 4m (excluded)
        -1,  // 7: 5m (excluded)
        -1,  // 8: 6m (excluded)
        -1,  // 9: 7m (excluded)
        -1,  // 10: 8m (excluded)
        4,   // 11: 9m
        5,   // 12: 1p
        6,   // 13: 2p
        7,   // 14: 3p
        8,   // 15: 4p
        9,   // 16: 5p
        10,  // 17: 6p
        11,  // 18: 7p
        12,  // 19: 8p
        13,  // 20: 9p
        14,  // 21: 1s
        15,  // 22: 2s
        16,  // 23: 3s
        17,  // 24: 4s
        18,  // 25: 5s
        19,  // 26: 6s
        20,  // 27: 7s
        21,  // 28: 8s
        22,  // 29: 9s
        23,  // 30: East Wind
        24,  // 31: South Wind
        25,  // 32: West Wind
        26   // 33: North Wind
    )
    
    /**
     * Reverse mapping from sanma index to registry index.
     */
    private val sanmaToRegistryMap: IntArray = intArrayOf(
        0,   // 0: 1 Dragon
        1,   // 1: 2 Dragon
        2,   // 2: 3 Dragon
        3,   // 3: 1m
        11,  // 4: 9m
        12,  // 5: 1p
        13,  // 6: 2p
        14,  // 7: 3p
        15,  // 8: 4p
        16,  // 9: 5p
        17,  // 10: 6p
        18,  // 11: 7p
        19,  // 12: 8p
        20,  // 13: 9p
        21,  // 14: 1s
        22,  // 15: 2s
        23,  // 16: 3s
        24,  // 17: 4s
        25,  // 18: 5s
        26,  // 19: 6s
        27,  // 20: 7s
        28,  // 21: 8s
        29,  // 22: 9s
        30,  // 23: East
        31,  // 24: South
        32,  // 25: West
        33   // 26: North
    )
    
    /**
     * Converts a registry tile index to sanma index.
     * @param registryIndex Index from TileTypeRegistry (0-33)
     * @return Sanma index (0-26), or null if tile is excluded in sanma
     */
    fun registryToSanma(registryIndex: Int): Int? {
        if (registryIndex < 0 || registryIndex >= REGISTRY_SIZE) return null
        val sanmaIndex = registryToSanmaMap[registryIndex]
        return if (sanmaIndex == -1) null else sanmaIndex
    }
    
    /**
     * Converts a sanma index to registry index.
     * @param sanmaIndex Sanma tile index (0-26)
     * @return Registry index (0-33)
     */
    fun sanmaToRegistry(sanmaIndex: Int): Int {
        require(sanmaIndex in 0 until SANMA_SIZE) { 
            "Sanma index out of range: $sanmaIndex" 
        }
        return sanmaToRegistryMap[sanmaIndex]
    }
    
    /**
     * Converts a tile value (1-9) and registry segment to sanma index.
     * @param tileTypeIndex The tile type segment index (Dragon=0, Man=1, Pin=2, Sou=3, Wind=4)
     * @param value The tile value (1-9 for suits, 1-4 for winds/dragons)
     * @return Sanma index, or null if excluded
     */
    fun fromTileValue(tileTypeIndex: Int, value: Int): Int? {
        val registryIndex = when (tileTypeIndex) {
            0 -> value - 1  // Dragon: 0-2
            1 -> value + 2  // Man: 3-11
            2 -> value + 11 // Pin: 12-20
            3 -> value + 20 // Sou: 21-29
            4 -> value + 29 // Wind: 30-33
            else -> return null
        }
        return registryToSanma(registryIndex)
    }
    
    /**
     * Checks if a registry index represents a tile used in sanma.
     */
    fun isValidSanmaTile(registryIndex: Int): Boolean {
        return registryToSanma(registryIndex) != null
    }
    
    /**
     * Returns all valid sanma tile indices.
     */
    fun getAllSanmaIndices(): IntArray {
        return IntArray(SANMA_SIZE) { it }
    }
    
    /**
     * Checks if a tile is aka-eligible (5p or 5s).
     * In sanma, 5p (sanma index 9) and 5s (sanma index 18) can be aka.
     */
    fun isAkaEligible(sanmaIndex: Int): Boolean {
        return sanmaIndex == 9 || sanmaIndex == 18  // 5p or 5s
    }
}
