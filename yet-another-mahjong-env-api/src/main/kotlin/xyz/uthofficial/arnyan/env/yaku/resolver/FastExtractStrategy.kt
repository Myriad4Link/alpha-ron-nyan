package xyz.uthofficial.arnyan.env.yaku.resolver

interface FastExtractStrategy {
    fun tryRemove(histogram: IntArray, index: Int): Boolean
    fun revert(histogram: IntArray, index: Int)
    val type: MentsuType
}