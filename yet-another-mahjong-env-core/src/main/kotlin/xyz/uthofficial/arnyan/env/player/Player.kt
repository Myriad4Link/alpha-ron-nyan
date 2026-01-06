package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tiles.Tile
import java.util.UUID

class Player {
    val id: UUID = UUID.randomUUID()
    val hand = mutableListOf<Tile>()
}