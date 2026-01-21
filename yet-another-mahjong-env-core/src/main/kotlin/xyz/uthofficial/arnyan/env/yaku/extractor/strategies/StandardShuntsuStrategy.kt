package xyz.uthofficial.arnyan.env.yaku.extractor.strategies

import xyz.uthofficial.arnyan.env.yaku.extractor.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.extractor.MentsuType
import xyz.uthofficial.arnyan.env.yaku.extractor.Shuntsu

class StandardShuntsuStrategy : FastExtractStrategy {
    override val type: MentsuType = Shuntsu

    override fun tryRemove(histogram: IntArray, index: Int): Boolean {
//        if (index > )
        if (histogram[index] >= 3) {
            histogram[index] -= 3
            return true
        }
        return false
    }

    override fun revert(histogram: IntArray, index: Int) {
        TODO("Not yet implemented")
    }
}
