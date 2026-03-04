package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu

internal fun Tile.toIndex(): Int = when (tileType) {
    xyz.uthofficial.arnyan.env.tile.Dragon -> value + (-1)
    xyz.uthofficial.arnyan.env.tile.Man -> value + 2
    xyz.uthofficial.arnyan.env.tile.Pin -> value + 11
    xyz.uthofficial.arnyan.env.tile.Sou -> value + 20
    xyz.uthofficial.arnyan.env.tile.Wind -> value + 29
    else -> -1
}

internal fun Int.isYaochuhai(): Boolean = TileTypeRegistry.yaochuhaiIndices.contains(this)

internal fun Int.isDragon(): Boolean = this in 0..2

internal fun Int.isWind(): Boolean = this in 30..33

internal fun Int.toStandardWind(): StandardWind? = when (this) {
    30 -> StandardWind.EAST
    31 -> StandardWind.SOUTH
    32 -> StandardWind.WEST
    33 -> StandardWind.NORTH
    else -> null
}

internal fun CompactMentsu.tileIndices(): List<Int> = listOf(tile1Index, tile2Index, tile3Index, tile4Index).take(tileCount)

internal fun CompactMentsu.isKoutsu(): Boolean = mentsuType == MentsuTypeRegistry.mentsuTypes[2]

internal fun CompactMentsu.isKantsu(): Boolean = mentsuType == MentsuTypeRegistry.mentsuTypes[0]

internal fun CompactMentsu.isShuntsu(): Boolean = mentsuType == MentsuTypeRegistry.mentsuTypes[3]

internal fun CompactMentsu.isToitsu(): Boolean = mentsuType == MentsuTypeRegistry.mentsuTypes[4]