package xyz.uthofficial.arnyan.env.tiles

data class Tile(
    val tileType: TileType,
    val value: Int,
    val isAka: Boolean = false
) {
    override fun toString(): String = "$tileType$value ${if (isAka) "(a)" else ""}"
}