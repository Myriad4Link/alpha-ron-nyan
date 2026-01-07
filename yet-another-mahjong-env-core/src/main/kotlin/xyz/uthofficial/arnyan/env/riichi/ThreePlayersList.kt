package xyz.uthofficial.arnyan.env.riichi

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.PlayerList

data class ThreePlayersList(val a: Player, val b: Player, val c: Player): PlayerList {
    override fun forEach(block: (Player) -> Unit) {
        block(a)
        block(b)
        block(c)
    }
}