package xyz.uthofficial.arnyan.env.yaku

interface YakuConfiguration {
    infix fun Int.han(block: () -> Unit)
    
    fun evaluate(context: YakuContext, partitions: List<LongArray>): List<Pair<Yaku<LongArray>, Int>>
}
