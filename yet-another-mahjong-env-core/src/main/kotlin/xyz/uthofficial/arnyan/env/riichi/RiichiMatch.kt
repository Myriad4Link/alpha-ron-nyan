package xyz.uthofficial.arnyan.env.riichi

import xyz.uthofficial.arnyan.env.Match
import xyz.uthofficial.arnyan.env.tiles.TileSetConfiguration
import xyz.uthofficial.arnyan.env.tiles.TileType.*

class RiichiMatch(private val playersList: ThreePlayersList) : Match {
    val configuration: TileSetConfiguration = TileSetConfiguration().setGroup {
        listOf(1, 9) of MAN
        1..9 of (PIN and SOU)
        1..4 of WIND
        1..3 of DRAGON
    } repeatFor 4 whereEvery { PIN and SOU } has 1 redDoraOn 5

    private var _isEnded: Boolean = false
    override var isEnded: Boolean
        get() = _isEnded
        private set(value) {
            _isEnded = value
        }

    override fun initialize(): Match {
        configuration.build() deal 13 randomlyTo playersList
        TODO("Not yet implemented")
    }
}