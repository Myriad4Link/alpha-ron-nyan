package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.tile.Tile

interface Mentsu {
    val mentsuType: MentsuType
    val isOpen: Boolean
    val tiles: List<Tile>
    val akas: List<Tile>
}