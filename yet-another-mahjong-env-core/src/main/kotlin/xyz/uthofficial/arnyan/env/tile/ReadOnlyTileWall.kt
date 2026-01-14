package xyz.uthofficial.arnyan.env.tile

interface ReadOnlyTileWall {
    val standardDealAmount: Int
    val tileWall: List<Tile>
    val size: Int
}