package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile

sealed class LastAction {
    object None : LastAction()
    data class Discard(val tile: Tile, val player: Player) : LastAction()
    data class Draw(val tile: Tile, val player: Player) : LastAction()
    data class Chii(val tile: Tile, val player: Player) : LastAction()
    data class Pon(val tile: Tile, val player: Player) : LastAction()
    data class Ron(val tile: Tile, val player: Player) : LastAction()
    data class TsuMo(val tile: Tile, val player: Player) : LastAction()
    data class Pass(val tile: Tile, val player: Player) : LastAction()
}