package xyz.uthofficial.arnyan.env.ruleset.base

import xyz.uthofficial.arnyan.env.yaku.YakuConfiguration

interface YakuRule {
    fun build() : YakuConfiguration
}