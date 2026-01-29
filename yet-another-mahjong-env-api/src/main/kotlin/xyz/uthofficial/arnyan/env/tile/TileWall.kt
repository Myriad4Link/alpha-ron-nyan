package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result

interface TileWall : ReadOnlyTileWall {
    fun add(tile: Tile): Boolean
    fun addAll(tiles: Collection<Tile>): Boolean
    fun shuffle()
    fun draw(amount: Int): Result<List<Tile>, WallError>

    interface Dealer {
        infix fun randomlyTo(players: List<Player>): Result<Unit, WallError>
    }

    infix fun deal(amount: Int): Dealer
}
