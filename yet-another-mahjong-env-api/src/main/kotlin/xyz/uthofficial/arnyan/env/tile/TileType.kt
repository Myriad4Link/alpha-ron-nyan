package xyz.uthofficial.arnyan.env.tile

interface TileType {
    val intRange: IntRange
    val isContinuous: Boolean
        get() = false
}
