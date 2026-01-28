package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Tile

class StandardFastTileResolver(vararg val strategies: FastExtractStrategy) {
    fun resolve(hand: List<Tile>): MutableList<List<MentsuType>> {
        val results = mutableListOf<List<MentsuType>>()
        backtrack(0, TileTypeRegistry.getHistogram(hand), mutableListOf(), results, hand)
        return results
    }

    private fun backtrack(
        startIndex: Int,
        histogram: IntArray,
        currentMentsu: MutableList<MentsuType>,
        results: MutableList<List<MentsuType>>,
        originalHand: List<Tile>
    ) {
        var i = startIndex
        while (i < TileTypeRegistry.SIZE && histogram[i] == 0) i++

        if (i >= TileTypeRegistry.SIZE) {
            results.add(ArrayList(currentMentsu))
            return
        }

        for (strategy in strategies) {
            if (strategy.tryRemove(histogram, i)) {
                currentMentsu.add(strategy.type)
                backtrack(i, histogram, currentMentsu, results, originalHand)
                currentMentsu.removeLast()
                strategy.revert(histogram, i)
            }
        }
    }

//    private fun rehydrate(structure)
}