package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.tile.ReadOnlyTileWall
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.Yaku
import xyz.uthofficial.arnyan.env.yaku.YakuConfiguration
import xyz.uthofficial.arnyan.env.yaku.YakuContext

object EmptyYakuConfiguration : YakuConfiguration {
    override infix fun Int.han(block: () -> Unit) {
        block()
    }

    override fun evaluate(context: YakuContext, partitions: List<LongArray>): List<Pair<Yaku<LongArray>, Int>> {
        return emptyList()
    }
}

data class MatchObservation(
    val players: List<ReadOnlyPlayer>,
    val wall: ReadOnlyTileWall,
    val topology: TableTopology,
    val currentSeatWind: Wind,
    val roundRotationStatus: RoundRotationStatus,
    val discards: Map<Wind, List<Tile>> = emptyMap(),
    val lastAction: LastAction = LastAction.None,
    val availableActions: List<Action> = emptyList(),
    val yakuConfiguration: YakuConfiguration = EmptyYakuConfiguration
)
