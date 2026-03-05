package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.yaku.isYaochuhai
import xyz.uthofficial.arnyan.env.yaku.toIndex
import xyz.uthofficial.arnyan.env.tile.*

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

    private fun findShuntsuContainingTile(mentsus: LongArray, tileIndex: Int): CompactMentsu? {
        for (packed in mentsus) {
            val mentsu = CompactMentsu(packed)
            if (mentsu.isShuntsu()) {
                val indices = listOf(mentsu.tile1Index, mentsu.tile2Index, mentsu.tile3Index)
                if (tileIndex in indices) {
                    return mentsu
                }
            }
        }
        return null
    }

    private fun isRyanmenWait(mentsu: CompactMentsu, winningTileIndex: Int): Boolean {
        val indices = listOf(mentsu.tile1Index, mentsu.tile2Index, mentsu.tile3Index).sorted()
        // winning tile must be edge of the sequence
        val winningPos = indices.indexOf(winningTileIndex)
        if (winningPos != 0 && winningPos != 2) return false // middle position (kanchan)
        
        // Get tile type and value to check if terminal (1 or 9)
        val tileType = TileTypeRegistry.getTileType(winningTileIndex)
        val segment = TileTypeRegistry.getSegment(tileType)
        val value = winningTileIndex - segment[0] + 1
        
        // Edge tile must not be terminal (1 or 9) for ryanmen wait
        // Terminal wait is penchan, which is not allowed for pinfu
        return value != 1 && value != 9
    }

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
                // Wait detection: must be ryanmen wait
                val winningTileIndex = context.winningTile.toIndex()
                val winningShuntsu = findShuntsuContainingTile(mentsus, winningTileIndex)
                if (winningShuntsu != null && isRyanmenWait(winningShuntsu, winningTileIndex)) {
                    return intArrayOf(1)
                }
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