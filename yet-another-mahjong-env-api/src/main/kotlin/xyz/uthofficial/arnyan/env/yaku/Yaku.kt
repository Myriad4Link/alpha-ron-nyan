package xyz.uthofficial.arnyan.env.yaku

interface Yaku<T> {
    val name: String
    fun judge(tiles: T): Boolean
}