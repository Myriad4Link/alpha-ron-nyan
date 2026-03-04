package xyz.uthofficial.arnyan.env.yaku

class StandardYakuConfiguration : YakuConfiguration {
    private val yakuHanMap = mutableMapOf<Yaku<LongArray>, Int>()
    private var currentHan: Int = 0

    override infix fun Int.han(block: () -> Unit) {
        currentHan = this
        block()
        currentHan = 0
    }

    fun yaku(yaku: Yaku<LongArray>) {
        yakuHanMap[yaku] = currentHan
    }

    override fun evaluate(context: YakuContext, partitions: List<LongArray>): List<Pair<Yaku<LongArray>, Int>> {
        val results = mutableListOf<Pair<Yaku<LongArray>, Int>>()
        for ((yaku, hanValue) in yakuHanMap) {
            // We need to check if yaku is applicable for any partition
            for (partition in partitions) {
                val judgeResult = yaku.judge(partition, context)
                if (judgeResult.isNotEmpty()) {
                    // For yaku that can have multiple counts (e.g., Yakuhai), judge returns array of han per occurrence
                    // We'll sum them and multiply by base han? Actually base han is already the han per occurrence.
                    // For simplicity, we'll just add base han for each occurrence.
                    val totalHan = judgeResult.sum() * hanValue
                    results.add(yaku to totalHan)
                    break
                }
            }
        }
        return results
    }

    fun isEmpty(): Boolean = yakuHanMap.isEmpty()
}