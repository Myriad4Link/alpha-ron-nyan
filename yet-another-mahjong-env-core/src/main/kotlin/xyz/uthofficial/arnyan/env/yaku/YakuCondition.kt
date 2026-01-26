package xyz.uthofficial.arnyan.env.yaku

fun interface YakuCondition {
    fun judge(tiles: IntArray): Boolean
}