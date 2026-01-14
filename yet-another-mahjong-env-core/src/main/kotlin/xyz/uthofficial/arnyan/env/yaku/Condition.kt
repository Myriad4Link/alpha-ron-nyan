package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileComposition

class Condition(private val condition: List<Tile>.() -> Condition) {
    fun isSatisfied(hand: List<Tile>) {
        TODO()
    }

    companion object {
        fun build(block: List<Tile>.() -> Condition): Condition = Condition(block)

        infix fun List<Tile>.have(composition: TileComposition): Condition {
            TODO()
        }
    }

}