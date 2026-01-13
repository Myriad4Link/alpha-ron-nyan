package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding

class TileWall {
    private val tiles = ArrayDeque<Tile>()
    val tileWall: List<Tile>
        get() = tiles

    val size
        get() = tiles.size

    fun add(tile: Tile) = tiles.add(tile)
    fun addAll(tile: Collection<Tile>) = tiles.addAll(tile)

    fun shuffle() = tiles.shuffle()

    fun draw(amount: Int): Result<List<Tile>, WallError> {
        if (tiles.size < amount) return Result.Failure(WallError.NotEnoughTiles(amount, tiles.size))
        return Result.Success(List(amount) { tiles.removeLast() })
    }

    infix fun deal(amount: Int): Dealer = Dealer(amount, this)

    class Dealer(private val amount: Int, private val wall: TileWall) {
        infix fun randomlyTo(players: List<Player>): Result<Unit, WallError> = binding {
            wall.shuffle()

            players.forEach {
                val drawn = wall.draw(amount).bind()
                it.hand.addAll(drawn)
            }
        }
    }
}