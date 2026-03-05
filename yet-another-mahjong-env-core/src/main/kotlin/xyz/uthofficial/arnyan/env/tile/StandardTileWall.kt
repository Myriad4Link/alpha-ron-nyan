package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding

class StandardTileWall(override val standardDealAmount: Int) : TileWall {
    private val tiles = ArrayDeque<Tile>()
    private var deadWallTiles: List<Tile> = emptyList()
    private var doraIndicatorIndex: Int = 0
    
    override val tileWall: List<Tile>
        get() = tiles + deadWallTiles

    override val size
        get() = tiles.size

    override val doraIndicators: List<Tile>
        get() {
            if (doraIndicatorIndex >= deadWallTiles.size) return emptyList()
            return listOf(deadWallTiles[doraIndicatorIndex])
        }

    override val deadWallRemaining: Int
        get() = deadWallTiles.size - doraIndicatorIndex

    override fun add(tile: Tile) = tiles.add(tile)
    override fun addAll(tiles: Collection<Tile>) = this@StandardTileWall.tiles.addAll(tiles)

    override fun shuffle() {
        tiles.shuffle()
        deadWallTiles = emptyList()
        doraIndicatorIndex = 0
    }

    override fun draw(amount: Int): Result<List<Tile>, WallError> {
        if (tiles.size < amount) return Result.Failure(WallError.NotEnoughTiles(amount, tiles.size))
        return Result.Success(List(amount) { tiles.removeLast() })
    }

    override fun initializeDeadWall(deadWallSize: Int) {
        if (tiles.size < deadWallSize) return
        
        deadWallTiles = tiles.takeLast(deadWallSize).reversed()
        repeat(deadWallSize) { tiles.removeLast() }
        doraIndicatorIndex = 0
    }

    override fun revealNextDoraIndicator(): Result<Tile, WallError> {
        if (doraIndicatorIndex + 1 >= deadWallTiles.size) {
            return Result.Failure(WallError.NotEnoughTiles(1, deadWallTiles.size - doraIndicatorIndex))
        }
        doraIndicatorIndex++
        return Result.Success(deadWallTiles[doraIndicatorIndex])
    }

    override infix fun deal(amount: Int): TileWall.Dealer = StandardDealer(amount, this)

    class StandardDealer(private val amount: Int, private val wall: StandardTileWall) : TileWall.Dealer {
        override infix fun randomlyTo(players: List<Player>): Result<Unit, WallError> = binding {
            if (wall.size >= 100) {
                wall.initializeDeadWall()
            }
            wall.shuffle()

            players.forEach {
                val drawn = wall.draw(amount).bind()
                it.closeHand.addAll(drawn)
            }
        }
    }
}
