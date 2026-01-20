package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding

class StandardTileWall(override val standardDealAmount: Int) : TileWall {
    private val tiles = ArrayDeque<Tile>()
    override val tileWall: List<Tile>
        get() = tiles

    override val size
        get() = tiles.size

    override fun add(tile: Tile) = tiles.add(tile)
    override fun addAll(tiles: Collection<Tile>) = this@StandardTileWall.tiles.addAll(tiles)

    override fun shuffle() = tiles.shuffle()

    override fun draw(amount: Int): Result<List<Tile>, WallError> {
        if (tiles.size < amount) return Result.Failure(WallError.NotEnoughTiles(amount, tiles.size))
        return Result.Success(List(amount) { tiles.removeLast() })
    }

    override infix fun deal(amount: Int): TileWall.Dealer = StandardDealer(amount, this)

    class StandardDealer(private val amount: Int, private val wall: StandardTileWall): TileWall.Dealer {
        override infix fun randomlyTo(players: List<Player>): Result<Unit, WallError> = binding {
            wall.shuffle()

            players.forEach {
                val drawn = wall.draw(amount).bind()
                it.hand.addAll(drawn)
            }
        }
    }
}