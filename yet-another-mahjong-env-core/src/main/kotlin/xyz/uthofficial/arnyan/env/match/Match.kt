package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.TileWall
import xyz.uthofficial.arnyan.env.wind.Wind

class Match(private val ruleSet: RuleSet) {
    lateinit var wall: TileWall

    fun initialise(players: List<Player>) {
        wall = ruleSet.wallGenerationRule.build()
        wall deal 13 randomlyTo players

        players.assignSeatRandomly()
    }

    fun next() {

    }

    fun List<Player>.assignSeatRandomly() {
        val winds = Wind.entries.shuffled()
        forEachIndexed { index, player ->
            player.seat = winds[index % winds.size]
        }
    }

    infix fun List<Player>.getPlayerSitAt(wind: Wind): Player = first { it.seat == wind }
}