package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

object StandardKoutsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Koutsu
    
    override val tileOffsets: IntArray = intArrayOf(0, 0, 0)

    override fun tryRemove(histogram: IntArray, index: Int): Boolean {
        if (histogram[index] >= 3) {
            histogram[index] -= 3
            return true
        }

        return false
    }

    override fun revert(histogram: IntArray, index: Int) {
        histogram[index] += 3
    }
}
