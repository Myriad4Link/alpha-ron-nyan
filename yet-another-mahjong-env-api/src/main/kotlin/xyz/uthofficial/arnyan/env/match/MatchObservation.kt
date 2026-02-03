package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.tile.ReadOnlyTileWall
import xyz.uthofficial.arnyan.env.wind.RoundWindCycle
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind

data class MatchObservation(
    val players: List<ReadOnlyPlayer>,
    val wall: ReadOnlyTileWall,
    val topology: TableTopology,
    val currentSeatWind: Wind,
    val roundWindCycle: RoundWindCycle
)
