package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.tile.Tile

@JvmInline
value class CompactMentsu(val raw: Long) : Mentsu {
    override val mentsuType: MentsuType
        get() = TODO("Not yet implemented")
    override val isOpen: Boolean
        get() = TODO("Not yet implemented")
    override val tiles: List<Tile>
        get() = TODO("Not yet implemented")
    override val akas: List<Tile>
        get() = TODO("Not yet implemented")

}