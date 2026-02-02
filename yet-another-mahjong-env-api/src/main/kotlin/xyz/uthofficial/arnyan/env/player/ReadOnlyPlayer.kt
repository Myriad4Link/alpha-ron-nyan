package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import java.util.*

interface ReadOnlyPlayer {
    val id: UUID
    val closeHand: List<Tile>
    val openHand: List<List<Tile>>
    val currentMentsusComposition: List<List<Mentsu>>
    val seat: Wind?
}

infix fun <T : ReadOnlyPlayer> List<T>.getPlayerSitAt(wind: Wind): T = first { it.seat == wind }