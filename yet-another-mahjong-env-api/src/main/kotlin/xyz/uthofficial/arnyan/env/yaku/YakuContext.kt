package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.tile.Tile

enum class WinningMethod {
    RON,
    TSUMO
}

data class YakuContext(
    val seatWind: Wind,
    val roundWind: Wind,
    val isOpenHand: Boolean,
    val isRiichiDeclared: Boolean,
    val winningTile: Tile,
    val winningMethod: WinningMethod
)