package xyz.uthofficial.arnyan.env.yaku

object Riichi : FastYaku {
    override val name: String
        get() = "Riichi"

    override fun judge(mentsus: LongArray, context: YakuContext?): IntArray {
        if (context?.isRiichiDeclared == true) {
            return intArrayOf(1)
        }
        return intArrayOf()
    }
}