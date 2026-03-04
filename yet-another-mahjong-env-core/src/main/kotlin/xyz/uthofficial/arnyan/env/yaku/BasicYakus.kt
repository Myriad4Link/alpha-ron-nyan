package xyz.uthofficial.arnyan.env.yaku

object Tanyao : FastYaku {
    override val name: String
        get() = "Tanyao"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        // TODO: Implement Tanyao detection - no yaochuhai tiles
        return intArrayOf()
    }
}

object Yakuhai : FastYaku {
    override val name: String
        get() = "Yakuhai"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        // TODO: Implement Yakuhai detection - dragons or seat/round wind
        return intArrayOf()
    }
}

object Pinfu : FastYaku {
    override val name: String
        get() = "Pinfu"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        // TODO: Implement Pinfu detection - all sequences, non-yaochuhai pair, edge wait
        return intArrayOf()
    }
}

object MenzenTsumo : FastYaku {
    override val name: String
        get() = "Menzen Tsumo"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        // TODO: Implement Menzen Tsumo detection - closed hand self-draw
        return intArrayOf()
    }
}