package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ArnyanError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.TileWall
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind

class Match(private val ruleSet: RuleSet) {
    lateinit var wall: TileWall
    lateinit var topology: TableTopology

    fun initialise(players: List<Player>): Result<Unit, ArnyanError> = binding {
        wall = ruleSet.wallGenerationRule.build().bind()
        (wall deal 13 randomlyTo players).bind()

        topology = ruleSet.playerWindRotationOrderRule.build().bind()
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