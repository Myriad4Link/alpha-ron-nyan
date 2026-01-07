package xyz.uthofficial.arnyan.env.tiles

import xyz.uthofficial.arnyan.env.player.PlayerList

class TileWall {
    private val tiles = ArrayDeque<Tile>()
    val tileWall: List<Tile>
        get() = tiles

    val size
        get() = tiles.size

    fun add(tile: Tile) = tiles.add(tile)

    fun shuffle() = tiles.shuffle()

    fun draw(amount: Int): Result<List<Tile>> = runCatching {
        List(amount) { tiles.removeLast() }
    }

    infix fun deal(amount: Int): Dealer = Dealer(amount, this)

    class Dealer(private val amount: Int, private val wall: TileWall) {
        infix fun randomlyTo(players: PlayerList): Result<Unit> = runCatching {
            wall.tiles.shuffle()

            players.forEach {
                val drawn = wall.draw(amount).getOrThrow()
                it.hand.addAll(drawn)
            }
        }
    }
}