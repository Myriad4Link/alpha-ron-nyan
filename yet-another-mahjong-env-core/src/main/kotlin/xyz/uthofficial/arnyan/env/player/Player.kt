package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import java.util.UUID

class Player(override val id: UUID = UUID.randomUUID()) : ReadOnlyPlayer {
    override val hand = mutableListOf<Tile>()
    override var seat: Wind? = null

}