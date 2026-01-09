package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import java.util.UUID

class Player {
    val id: UUID = UUID.randomUUID()
    val hand = mutableListOf<Tile>()
    var seat: Wind? = null
}