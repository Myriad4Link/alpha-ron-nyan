package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

object StandardKoutsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Koutsu
    override val mentsuAmount: Int = 3

    override fun tryRemove(histogram: IntArray, index: Int): IntArray? {
        if (histogram[index] >= 3) {
            histogram[index] -= 3
            return intArrayOf(index, index, index)
        }

        return null
    }

    override fun revert(histogram: IntArray, removedIndices: IntArray) {
        histogram[removedIndices[0]] += 3
    }
}
