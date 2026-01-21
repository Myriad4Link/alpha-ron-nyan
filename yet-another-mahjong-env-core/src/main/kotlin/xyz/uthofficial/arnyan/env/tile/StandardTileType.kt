package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType

sealed interface StandardTileType : TileType

@RegisterTileType
object Man : StandardTileType {
    override val intRange: IntRange = 1..9
}

@RegisterTileType
object Sou : StandardTileType {
    override val intRange: IntRange = 1..9
}

@RegisterTileType
object Pin : StandardTileType {
    override val intRange: IntRange = 1..9
}

@RegisterTileType
object Wind : StandardTileType {
    override val intRange: IntRange = 1..4
}

@RegisterTileType
object Dragon : StandardTileType {
    override val intRange: IntRange = 1..3
}