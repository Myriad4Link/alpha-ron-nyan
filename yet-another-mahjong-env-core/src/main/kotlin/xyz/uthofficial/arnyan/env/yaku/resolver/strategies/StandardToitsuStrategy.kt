package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu

object StandardToitsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Toitsu

    override val mentsuAmount: Int = 2

    override fun tryRemove(histogram: IntArray, index: Int): IntArray? {
        if (histogram[index] >= 2) {
            histogram[index] -= 2
            return intArrayOf(index, index)
        }
        return null
    }

    override fun revert(histogram: IntArray, removedIndices: IntArray) {
        histogram[removedIndices[0]] += 2
    }
}