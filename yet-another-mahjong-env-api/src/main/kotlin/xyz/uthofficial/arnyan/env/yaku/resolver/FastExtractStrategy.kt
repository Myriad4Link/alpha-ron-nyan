package xyz.uthofficial.arnyan.env.yaku.resolver

interface FastExtractStrategy {
    fun tryRemove(histogram: IntArray, index: Int): IntArray?
    fun revert(histogram: IntArray, removedIndices: IntArray)
    val type: MentsuType
    val mentsuAmount: Int
}