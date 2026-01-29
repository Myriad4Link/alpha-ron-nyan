package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu

object StandardToitsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Toitsu

    override val tileOffsets: IntArray = intArrayOf(0, 0)

    override fun tryRemove(histogram: IntArray, index: Int): Boolean {
        if (histogram[index] >= 2) {
            histogram[index] -= 2
            return true
        }
        return false
    }

    override fun revert(histogram: IntArray, index: Int) {
        histogram[index] += 2
    }
}