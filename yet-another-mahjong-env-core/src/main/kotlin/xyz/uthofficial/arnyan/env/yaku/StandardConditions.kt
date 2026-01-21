package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.dsl.of
import xyz.uthofficial.arnyan.env.yaku.YakuCondition.Companion.haveNone

val Tanyao = YakuCondition.build {
    this haveNone (listOf(1, 9) of Sou)
}