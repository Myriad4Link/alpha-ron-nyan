package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.TileWall

internal fun MatchObservation.toState(): MatchState = MatchState(
    players = players.map { it as Player },
    wall = wall as TileWall,
    topology = topology,
    currentSeatWind = currentSeatWind,
    roundRotationStatus = roundRotationStatus,
    discards = discards.mapValues { (_, list) -> list.toMutableList() }.toMutableMap(),
    lastAction = lastAction,
    yakuConfiguration = yakuConfiguration,
    scoringCalculator = scoringCalculator,
    furitenPlayers = furitenPlayers.toMutableSet(),
    temporaryFuritenPlayers = temporaryFuritenPlayers.toMutableSet(),
    riichiSticks = riichiSticks,
    honbaSticks = honbaSticks
)
