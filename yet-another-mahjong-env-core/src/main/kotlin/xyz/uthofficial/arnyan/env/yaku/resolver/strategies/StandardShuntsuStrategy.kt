package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu

object StandardShuntsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Shuntsu
    override val mentsuAmount: Int = 3
    override fun tryRemove(histogram: IntArray, index: Int): IntArray? {
        if (index > histogram.size - 3) return null
        val mask = TileTypeRegistry.connectivityMask[index]
        if (mask > 0 || mask != TileTypeRegistry.connectivityMask[index + 2]) return null
        if (histogram[index] > 0 && histogram[index + 1] > 0 && histogram[index + 2] > 0) {
            histogram[index]--
            histogram[index + 1]--
            histogram[index + 2]--
            return intArrayOf(index, index + 1, index + 2)
        }
        return null
    }

    override fun revert(histogram: IntArray, removedIndices: IntArray) {
        histogram[removedIndices[0]]++
        histogram[removedIndices[1]]++
        histogram[removedIndices[2]]++
    }
}