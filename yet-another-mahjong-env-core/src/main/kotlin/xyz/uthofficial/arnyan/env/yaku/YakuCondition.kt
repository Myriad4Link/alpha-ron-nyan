package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileComposition

class YakuCondition(private val condition: List<Tile>.() -> Boolean) {
    val dependentConditions = mutableListOf<YakuCondition>()

    fun subscribeToPreconditions(conditions: List<YakuCondition>) =
        conditions.forEach { it.dependentConditions.add(this) }

    var isPreconditionSatisfied = true

    fun check(hand: List<Tile>): Boolean {
        if (!isPreconditionSatisfied) return false
        return condition(hand)
    }

    companion object {
        fun build(block: List<Tile>.() -> Boolean): YakuCondition = YakuCondition(block)

        infix fun List<Tile>.have(composition: TileComposition): Boolean {
            return this.any { tile ->
                composition.composition.any { (tileType, values) ->
                    tile.tileType == tileType && values.any { it == tile.value }
                }
            }
        }

        infix fun List<Tile>.haveNone(composition: TileComposition): Boolean = !this.have(composition)
    }

}