package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.getPlayerSitAt
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.TileWall
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind

class Match private constructor(
    private val listeners: List<MatchListener>,
    private val players: List<Player>,
    var wall: TileWall,
    val topology: TableTopology,
    private var currentSeatWind: Wind
) {
    fun start() = binding {
        players.getPlayerSitAt(currentSeatWind).closeHand.add(wall.draw(1).bind().first())
        val currentState = observation
        listeners.forEach { it.onMatchStarted(currentState) }
        StepResult(currentState, topology.getShimocha(currentSeatWind).bind(), false)
    }

    private fun next() {
    }

    fun checkOver(): Boolean = TODO()

    val observation: MatchObservation
        get() = MatchObservation(
            players = players,
            wall = wall,
            topology = topology,
            currentSeatWind = currentSeatWind
        )

    companion object {
        fun create(
            ruleSet: RuleSet,
            listeners: List<MatchListener>,
            playerList: List<Player>,
            shuffleWinds: Boolean
        ) = binding {
            val wall = ruleSet.wallGenerationRule.build().bind()
            (wall deal wall.standardDealAmount randomlyTo playerList).bind()

            val topology = ruleSet.playerWindRotationOrderRule.build().bind()

            if (shuffleWinds)
                playerList.assignSeatRandomly(topology)
            else
                playerList.assignSeatInOrder(topology)

            val currentSeatWind = topology.firstSeatWind

            val match = Match(
                listeners,
                playerList,
                wall,
                topology,
                currentSeatWind
            )

            listeners.forEach { it.onMatchStarted(match.observation) }
            match
        }

        private fun List<Player>.assignSeatRandomly(topology: TableTopology) {
            val winds = topology.seats.shuffled()
            forEachIndexed { index, player ->
                player.seat = winds[index % winds.size]
            }
        }

        private fun List<Player>.assignSeatInOrder(topology: TableTopology) {
            val winds = topology.seats
            forEachIndexed { index, player ->
                player.seat = winds[index % winds.size]
            }
        }
    }
}
