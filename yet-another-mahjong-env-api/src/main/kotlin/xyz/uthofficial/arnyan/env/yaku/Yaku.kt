package xyz.uthofficial.arnyan.env.yaku

interface Yaku<MentsusType> {
    val name: String
    fun judge(mentsus: MentsusType): IntArray
}