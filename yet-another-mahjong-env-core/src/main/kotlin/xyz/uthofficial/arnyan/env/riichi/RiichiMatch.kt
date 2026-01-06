package xyz.uthofficial.arnyan.env.riichi

import xyz.uthofficial.arnyan.env.Match
import xyz.uthofficial.arnyan.env.TileSetConfiguration
import xyz.uthofficial.arnyan.env.tiles.TileType.*

class RiichiMatch(private val playersList: ThreePlayersList) : Match {
    val configuration: TileSetConfiguration = TileSetConfiguration().setGroup {
        listOf(1, 9) of MAN
        1..9 of (PIN and SOU)
        1..4 of WIND
        1..3 of DRAGON
    } repeat 4

    override var isEnded: Boolean
        get() = TODO("Not yet implemented")
        private set(value) = TODO()

    override fun initialize(): Match {
        playersList.forEach {

        }
        TODO("Not yet implemented")
    }

    override fun deal(randomSeed: Int): Match {
        TODO("Not yet implemented")
    }

}