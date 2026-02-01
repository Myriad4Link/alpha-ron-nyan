package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

object StandardKantsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Kantsu
    override val mentsuAmount: Int = 4

    override fun tryRemove(histogram: IntArray, index: Int): IntArray? {
        if (histogram[index] >= 4) {
            histogram[index] -= 4
            return intArrayOf(index, index, index, index)
        }
        return null
    }

    override fun revert(histogram: IntArray, removedIndices: IntArray) {
        histogram[removedIndices[0]] += 4
    }

}