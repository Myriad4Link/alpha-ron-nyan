package xyz.uthofficial.arnyan.env.player

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu

interface Player : ReadOnlyPlayer {
    override val closeHand: MutableList<Tile>
    override val openHand: MutableList<List<Tile>>
    override val currentMentsusComposition: MutableList<List<Mentsu>>
    override var seat: Wind?
}
