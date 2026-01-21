package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

object StandardKantsuStrategy : FastExtractStrategy {
    override fun tryRemove(histogram: IntArray, index: Int): Boolean {
        if (histogram[index] >= 4) {
            histogram[index] -= 4
            return true
        }
        return false
    }

    override fun revert(histogram: IntArray, index: Int) {
        histogram[index] += 4
    }

    override val type: MentsuType
        get() = Kantsu

}