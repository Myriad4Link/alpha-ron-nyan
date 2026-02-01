package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Tile

class StandardFastTileResolver(vararg val strategies: FastExtractStrategy) {
    private val minTileCount = strategies.minOf { it.mentsuAmount }

    private val histogramBuffer = IntArray(TileTypeRegistry.SIZE)
    private var mentsuBuffer = LongArray(0)

    fun resolve(hand: List<Tile>): List<LongArray> {
        TileTypeRegistry.getHistogram(hand, histogramBuffer)

        val maxMentsu = hand.size / minTileCount
        if (mentsuBuffer.size < maxMentsu) mentsuBuffer = LongArray(maxMentsu)

        val results = mutableListOf<LongArray>()
        backtrack(0, histogramBuffer, mentsuBuffer, 0, results)
        return results
    }

    fun resolve(histogram: IntArray): List<LongArray> {
        histogram.copyInto(histogramBuffer)

        val totalTiles = histogram.sum()
        val maxMentsu = totalTiles / minTileCount
        if (mentsuBuffer.size < maxMentsu) mentsuBuffer = LongArray(maxMentsu)

        val results = mutableListOf<LongArray>()
        backtrack(0, histogramBuffer, mentsuBuffer, 0, results)
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
            val removedIndices = strategy.tryRemove(histogram, i)
            if (removedIndices != null) {
                val packed = packMentsu(strategy, removedIndices)
                buffer[depth] = packed
                backtrack(i, histogram, buffer, depth + 1, results)
                strategy.revert(histogram, removedIndices)
            }
        }
    }

    private fun packMentsu(strategy: FastExtractStrategy, removedIndices: IntArray): Long {
        val mentsuTypeIndex = MentsuTypeRegistry.getIndex(strategy.type)
        val containsYaochuhai = removedIndices.any { TileTypeRegistry.yaochuhaiIndices.contains(it) }
        return CompactMentsu.pack(
            removedIndices,
            mentsuTypeIndex,
            isOpen = false,
            containsYaochuhai = containsYaochuhai
        )
    }
}