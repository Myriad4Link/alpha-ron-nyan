package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.WinningMethod

object Tanyao : FastYaku {
    override val name: String
        get() = "Tanyao"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        for (packed in mentsus) {
            val mentsu = CompactMentsu(packed)
            if (mentsu.containsYaochuhai) {
                return intArrayOf()
            }
        }
        return intArrayOf(1)
    }
}

object Yakuhai : FastYaku {
    override val name: String
        get() = "Yakuhai"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        val matches = mutableListOf<Int>()
        for (packed in mentsus) {
            val mentsu = CompactMentsu(packed)
            if (!mentsu.isKoutsu() && !mentsu.isKantsu()) continue
            val tileIndex = mentsu.tile1Index
            if (tileIndex.isDragon()) {
                matches.add(1)
                continue
            }
            if (tileIndex.isWind() && context != null) {
                val tileWind = tileIndex.toStandardWind()
                if (tileWind == context.seatWind || tileWind == context.roundWind) {
                    matches.add(1)
                }
            }
        }
        return matches.toIntArray()
    }
}

object Pinfu : FastYaku {
    override val name: String
        get() = "Pinfu"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        if (context == null || context.isOpenHand) return intArrayOf()
        if (mentsus.size != 5) return intArrayOf()
        
        var shuntsuCount = 0
        var pairMentsu: CompactMentsu? = null
        
        for (packed in mentsus) {
            val mentsu = CompactMentsu(packed)
            when {
                mentsu.isShuntsu() -> shuntsuCount++
                mentsu.isToitsu() -> pairMentsu = mentsu
                else -> return intArrayOf() // contains koutsu/kantsu
            }
        }
        
        if (shuntsuCount == 4 && pairMentsu != null) {
            val pairTileIndex = pairMentsu.tile1Index
            // Pair must not be yaochuhai (terminals or honors)
            if (!pairTileIndex.isYaochuhai()) {
                // TODO: Wait detection (should be ryanmen)
                // For now, assume wait is valid
                return intArrayOf(1)
            }
        }
        return intArrayOf()
    }
}

object MenzenTsumo : FastYaku {
    override val name: String
        get() = "Menzen Tsumo"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        if (context == null) return intArrayOf()
        if (!context.isOpenHand && context.winningMethod == WinningMethod.TSUMO) {
            return intArrayOf(1)
        }
        return intArrayOf()
    }
}