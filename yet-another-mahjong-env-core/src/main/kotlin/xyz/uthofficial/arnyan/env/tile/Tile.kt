package xyz.uthofficial.arnyan.env.tile

data class Tile(
    val tileType: TileType,
    val value: Int,
    var isAka: Boolean = false
) {
    override fun toString(): String = "$tileType$value ${if (isAka) "(a)" else ""}"
}