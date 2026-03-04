package xyz.uthofficial.arnyan.env.yaku

object Riichi : FastYaku {
    override val name: String
        get() = "Riichi"

    override fun judge(mentsus: LongArray): IntArray {
        // TODO: Implement riichi detection based on game state
        // Riichi yaku requires player to have declared riichi, which is not captured in mentsus.
        // Returning empty array indicates yaku not applicable based on hand composition alone.
        return intArrayOf()
    }
}