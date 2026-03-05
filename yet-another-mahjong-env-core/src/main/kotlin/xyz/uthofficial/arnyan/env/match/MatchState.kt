package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileWall
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.YakuConfiguration
import xyz.uthofficial.arnyan.env.scoring.ScoringCalculator

internal data class MatchState(
    val players: List<Player>,
    var wall: TileWall,
    val topology: TableTopology,
    var currentSeatWind: Wind,
    var roundRotationStatus: RoundRotationStatus,
    val yakuConfiguration: YakuConfiguration,
    val scoringCalculator: ScoringCalculator,
    val discards: MutableMap<Wind, MutableList<Tile>> = mutableMapOf(),
    var lastAction: LastAction = LastAction.None,
    val availableActionsMaskPerPlayer: MutableMap<Wind, Int> = mutableMapOf(),
    val passedPlayers: MutableSet<Wind> = mutableSetOf(),
    val furitenPlayers: MutableSet<Wind> = mutableSetOf(),
    val temporaryFuritenPlayers: MutableSet<Wind> = mutableSetOf(),
    var riichiSticks: Int = 0,
    var honbaSticks: Int = 0
) {
    init {
        topology.seats.forEach {
            discards[it] = mutableListOf()
            availableActionsMaskPerPlayer[it] = 0
        }
    }

    fun toObservation(): MatchObservation = MatchObservation(
        players = players,
        wall = wall,
        topology = topology,
        currentSeatWind = currentSeatWind,
        roundRotationStatus = roundRotationStatus,
        discards = discards.mapValues { (_, list) -> list.toList() },
        lastAction = lastAction,
        yakuConfiguration = yakuConfiguration,
        scoringCalculator = scoringCalculator,
        riichiSticks = riichiSticks,
        honbaSticks = honbaSticks,
        furitenPlayers = furitenPlayers.toSet(),
        temporaryFuritenPlayers = temporaryFuritenPlayers.toSet()
    )
}