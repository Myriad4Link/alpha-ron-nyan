package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

class StandardFastTenpaiEvaluator(private val resolver: StandardFastTileResolver) {

    private val validMentsuTypes = setOf(Shuntsu, Koutsu, Kantsu)

    fun evaluate(histogram: IntArray): Map<Int, List<LongArray>> {
        val results = mutableMapOf<Int, List<LongArray>>()

        for (waitingIndex in 0 until TileTypeRegistry.SIZE) {
            if (histogram[waitingIndex] >= 4) continue

            histogram[waitingIndex]++
            val mentsuArrangements = resolver.resolve(histogram)
            histogram[waitingIndex]--

            val validArrangements = mentsuArrangements.filter { isTenpaiMentsus(it) }
            if (validArrangements.isNotEmpty()) {
                results[waitingIndex] = validArrangements
            }
        }

        return results
    }

    private fun isTenpaiMentsus(mentsus: LongArray): Boolean {
        when (mentsus.size) {
            1 -> {
                return CompactMentsu(mentsus[0]).mentsuType === Kokushi
            }
            5 -> {
                val toitsuCount = mentsus.count { CompactMentsu(it).mentsuType === Toitsu }
                if (toitsuCount != 1) return false
                // remaining four must be Shuntsu, Koutsu, or Kantsu
                return mentsus.all { mentsu ->
                    val type = CompactMentsu(mentsu).mentsuType
                    type === Toitsu || type in validMentsuTypes
                }
            }

            7 -> {
                return mentsus.all { CompactMentsu(it).mentsuType === Toitsu }
            }

            else -> return false
        }
    }
}