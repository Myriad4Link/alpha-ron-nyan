package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType


@RegisterTileType
object Man : TileType {
    override val intRange: IntRange = 1..9
    override val isContinuous: Boolean
        get() = true
}

@RegisterTileType
object Sou : TileType {
    override val intRange: IntRange = 1..9
    override val isContinuous: Boolean
        get() = true
}

@RegisterTileType
object Pin : TileType {
    override val intRange: IntRange = 1..9
    override val isContinuous: Boolean
        get() = true
}

@RegisterTileType
object Wind : TileType {
    override val intRange: IntRange = 1..4
}

@RegisterTileType
object Dragon : TileType {
    override val intRange: IntRange = 1..3
}