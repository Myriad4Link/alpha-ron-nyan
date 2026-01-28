package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileType

/**
 * Compact 64‑bit representation of a mentsu (tile group).
 *
 * Implements the [Mentsu] interface while storing all data in a single `Long`.
 * Use [pack] to create instances.
 *
 * @property raw the packed 64‑bit value
 */
@JvmInline
value class CompactMentsu(val raw: Long) : Mentsu {
    companion object {
        private const val TILE1_SHIFT = 0
        private const val TILE2_SHIFT = 8
        private const val TILE3_SHIFT = 16
        private const val TILE4_SHIFT = 24
        private const val TYPE_SHIFT = 32
        private const val OPEN_SHIFT = 40
        private const val TILE_COUNT_SHIFT = 48
        
        private const val TILE_MASK = 0xFFL
        private const val TYPE_MASK = 0xFFL
        private const val OPEN_MASK = 0x1L
        private const val TILE_COUNT_MASK = 0x3L
        

         
            /**
             * Packs a mentsu defined by tile offsets from a base index.
             *
             * ### Bit Layout (LSB first):
             * - Bits 0-7: Tile 1 index (0-255)
             * - Bits 8-15: Tile 2 index (0-255)
             * - Bits 16-23: Tile 3 index (0-255)
             * - Bits 24-31: Tile 4 index (0-255)
             * - Bits 32-39: Mentsu type index (0-255)
             * - Bit 40: Open flag (0=closed, 1=open)
             * - Bits 41-47: Reserved (unused)
             * - Bits 48-49: Tile count (1-4)
             *
             * ### Restrictions (caller must ensure):
             * - `tileOffsets.size` ≤ 4 (extra indices cause IllegalStateException)
             * - Each tile index (`baseIndex + offset`) fits within 8 bits (0-255) – higher bits are masked
             * - `mentsuTypeIndex` fits within 8 bits (0-255) – higher bits are masked
             *
             * @param tileOffsets offsets from baseIndex (size 0-4)
             * @param baseIndex starting tile index
             * @param mentsuTypeIndex index of mentsu type in registry
             * @param isOpen whether the mentsu is open (called from discard)
             * @return packed 64-bit representation
             */
            fun pack(
                tileOffsets: IntArray,
                baseIndex: Int,
                mentsuTypeIndex: Int,
                isOpen: Boolean = false
            ): Long {
                val tileCount = tileOffsets.size
                var result = 0L
                for (i in tileOffsets.indices) {
                    val tileIndex = baseIndex + tileOffsets[i]
                    val shift = when (i) {
                        0 -> TILE1_SHIFT
                        1 -> TILE2_SHIFT
                        2 -> TILE3_SHIFT
                        3 -> TILE4_SHIFT
                        else -> error("Unexpected tile index")
                    }
                    result = result or ((tileIndex.toLong() and TILE_MASK) shl shift)
                }
                result = result or ((mentsuTypeIndex.toLong() and TYPE_MASK) shl TYPE_SHIFT)
                result = result or ((if (isOpen) 1L else 0L) shl OPEN_SHIFT)
                result = result or ((tileCount.toLong() and TILE_COUNT_MASK) shl TILE_COUNT_SHIFT)
                return result
            }
    }
    
    private fun tileIndex(shift: Int): Int = ((raw shr shift) and TILE_MASK).toInt()
    
    val tile1Index: Int get() = tileIndex(TILE1_SHIFT)
    val tile2Index: Int get() = tileIndex(TILE2_SHIFT)
    val tile3Index: Int get() = tileIndex(TILE3_SHIFT)
    val tile4Index: Int get() = tileIndex(TILE4_SHIFT)
    
    private val mentsuTypeIndex: Int get() = ((raw shr TYPE_SHIFT) and TYPE_MASK).toInt()
    private val tileCount: Int get() = ((raw shr TILE_COUNT_SHIFT) and TILE_COUNT_MASK).toInt()
    
    override val mentsuType: MentsuType
        get() = MentsuTypeRegistry.getMentsuType(mentsuTypeIndex)
    
    override val isOpen: Boolean
        get() = ((raw shr OPEN_SHIFT) and OPEN_MASK) != 0L
    
     override val tiles: List<Tile>
         get() {
             val allIndices = listOf(tile1Index, tile2Index, tile3Index, tile4Index)
             return allIndices.take(tileCount).map { index -> indexToTile(index, false) }
         }
    
     override val akas: List<Tile>
         get() = emptyList()
    
     private fun indexToTile(index: Int, isAka: Boolean = false): Tile {
         val tileType = TileTypeRegistry.getTileType(index)
         val segment = TileTypeRegistry.getSegment(tileType)
         val value = index - segment[0] + 1
         return Tile(tileType, value, isAka)
     }
}