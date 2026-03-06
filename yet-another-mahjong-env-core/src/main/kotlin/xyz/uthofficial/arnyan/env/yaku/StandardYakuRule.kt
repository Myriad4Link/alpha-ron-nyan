package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.ruleset.base.YakuRule

object StandardYakuRule : YakuRule {
    override fun build(): StandardYakuConfiguration {
        return StandardYakuConfiguration().apply {
            // Basic yaku for sanma
            1 han { yaku(Tanyao) }
            1 han { yaku(Yakuhai) }
            1 han { yaku(Pinfu) }
            1 han { yaku(MenzenTsumo) }
            1 han { yaku(Riichi) }
        }
    }
}