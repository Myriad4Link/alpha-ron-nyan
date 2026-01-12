package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.TileWall
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind

class Match(private val ruleSet: RuleSet) {
    lateinit var wall: TileWall
    lateinit var topology: TableTopology

    fun initialise(players: List<Player>) {
        wall = ruleSet.wallGenerationRule.build().getOrThrow()
        wall deal 13 randomlyTo players

        topology = ruleSet.playerWindRotationOrderRule.build().getOrThrow()
        players.assignSeatRandomly()
    }

    fun next() {

    }

    fun List<Player>.assignSeatRandomly() {
        val winds = topology.seats.shuffled()
        forEachIndexed { index, player ->
            player.seat = winds[index % winds.size]
        }
    }

    infix fun List<Player>.getPlayerSitAt(wind: Wind): Player = first { it.seat == wind }
}