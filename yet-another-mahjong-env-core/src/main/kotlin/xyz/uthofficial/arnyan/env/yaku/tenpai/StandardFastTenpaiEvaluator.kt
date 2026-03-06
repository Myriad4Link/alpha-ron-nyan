package xyz.uthofficial.arnyan.env.yaku.tenpai

import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.yaku.resolver.*

class StandardFastTenpaiEvaluator(private val resolver: TileResolver<IntArray, List<LongArray>>) :
    TenpaiEvaluator<IntArray, Map<Int, List<LongArray>>> {

    private val validMentsuTypes = setOf(Shuntsu, Koutsu, Kantsu)

    override fun evaluate(hand: IntArray): Map<Int, List<LongArray>> {
        val results = mutableMapOf<Int, List<LongArray>>()

        for (waitingIndex in 0 until TileTypeRegistry.SIZE) {
            if (hand[waitingIndex] >= 4) continue

            hand[waitingIndex]++
            val mentsuArrangements = resolver.resolve(hand)
            hand[waitingIndex]--

            val validArrangements = mentsuArrangements.filter { isTenpaiMentsus(it) }
            if (validArrangements.isNotEmpty()) {
                results[waitingIndex] = validArrangements
            }
        }

        return results
    }

    internal fun isTenpaiMentsus(mentsus: LongArray): Boolean {
        when (mentsus.size) {
            1 -> {
                return CompactMentsu(mentsus[0]).mentsuType === Kokushi
            }

            5 -> {
                return mentsus.count { CompactMentsu(it).mentsuType === Toitsu } == 1 &&
                        mentsus.all { mentsu ->
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