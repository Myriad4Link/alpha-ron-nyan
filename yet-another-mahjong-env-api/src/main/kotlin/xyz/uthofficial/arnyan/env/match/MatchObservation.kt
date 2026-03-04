package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.tile.ReadOnlyTileWall
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind

data class MatchObservation(
    val players: List<ReadOnlyPlayer>,
    val wall: ReadOnlyTileWall,
    val topology: TableTopology,
    val currentSeatWind: Wind,
    val roundRotationStatus: RoundRotationStatus,
    val discards: Map<Wind, List<Tile>> = emptyMap(),
    val lastAction: LastAction = LastAction.None,
    val availableActions: List<Action> = emptyList()
)
