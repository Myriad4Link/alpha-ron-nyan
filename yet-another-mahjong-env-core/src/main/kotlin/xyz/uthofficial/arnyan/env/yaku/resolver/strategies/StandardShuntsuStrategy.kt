package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.yaku.resolver.FastExtractStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu

object StandardShuntsuStrategy : FastExtractStrategy {
    override fun tryRemove(histogram: IntArray, index: Int): Boolean {
        if (index > histogram.size - 3) return false
        val mask = TileTypeRegistry.connectivityMask[index]
        if (mask > 0 || mask != TileTypeRegistry.connectivityMask[index + 2]) return false
        if (histogram[index] > 0 && histogram[index + 1] > 0 && histogram[index + 2] > 0) {
            histogram[index]--
            histogram[index + 1]--
            histogram[index + 2]--
            return true
        }
        return false
    }

    override fun revert(histogram: IntArray, index: Int) {
        histogram[index]++
        histogram[index + 1]++
        histogram[index + 2]++
    }

    override val type: MentsuType = Shuntsu
    
    override val tileOffsets: IntArray = intArrayOf(0, 1, 2)
}