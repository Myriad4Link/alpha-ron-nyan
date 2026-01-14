package xyz.uthofficial.arnyan.env.tile.dsl

import xyz.uthofficial.arnyan.env.tile.TileComposition
import xyz.uthofficial.arnyan.env.tile.TileType

infix fun Iterable<Int>.of(tileType: TileType): TileComposition =
    TileComposition(mapOf(tileType to this.toList()))

infix fun Iterable<Int>.of(tileTypes: Iterable<TileType>): TileComposition {
    val numbers = this.toList()
    return TileComposition(tileTypes.associateWith { numbers })
}

fun allOf(tileType: TileType): TileComposition = tileType.intRange of tileType

fun allOf(tileTypes: Iterable<TileType>): TileComposition =
    tileTypes
        .map { it.intRange of it }
        .fold(TileComposition()) { acc, tileComposition -> acc + tileComposition }

infix fun TileType.and(operand: TileType): List<TileType> = listOf(this, operand)

infix fun List<TileType>.and(operand: TileType): List<TileType> = this + operand
