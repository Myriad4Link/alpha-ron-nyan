package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import java.util.UUID

interface ReadOnlyPlayer {
    val id: UUID
    val hand: List<Tile>
    val seat: Wind?
}

infix fun <T : ReadOnlyPlayer> List<T>.getPlayerSitAt(wind: Wind): T = first { it.seat == wind }