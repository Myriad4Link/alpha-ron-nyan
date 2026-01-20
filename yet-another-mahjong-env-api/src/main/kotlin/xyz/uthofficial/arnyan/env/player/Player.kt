package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind

interface Player : ReadOnlyPlayer {
    override val hand: MutableList<Tile>
    override var seat: Wind?
}
