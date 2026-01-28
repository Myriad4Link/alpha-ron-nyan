package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Tile

class StandardFastTileResolver(vararg val strategies: FastExtractStrategy) {
    fun resolve(hand: List<Tile>): List<LongArray> {
        val histogram = TileTypeRegistry.getHistogram(hand)
        val maxMentsu = hand.size / 3
        val buffer = LongArray(maxMentsu)
        val results = mutableListOf<LongArray>()
        backtrack(0, histogram, buffer, 0, results)
        return results
    }

    private fun backtrack(
        startIndex: Int,
        histogram: IntArray,
        buffer: LongArray,
        depth: Int,
        results: MutableList<LongArray>
    ) {
        var i = startIndex
        while (i < TileTypeRegistry.SIZE && histogram[i] == 0) i++

        if (i >= TileTypeRegistry.SIZE) {
            results.add(buffer.copyOf(depth))
            return
        }

        for (strategy in strategies) {
            if (strategy.tryRemove(histogram, i)) {
                val packed = packMentsu(strategy, i)
                buffer[depth] = packed
                backtrack(i, histogram, buffer, depth + 1, results)
                strategy.revert(histogram, i)
            }
        }
    }

     private fun packMentsu(strategy: FastExtractStrategy, baseIndex: Int): Long {
          val mentsuTypeIndex = MentsuTypeRegistry.getIndex(strategy.type)
           return CompactMentsu.pack(strategy.tileOffsets, baseIndex, mentsuTypeIndex, isOpen = false)
     }
}