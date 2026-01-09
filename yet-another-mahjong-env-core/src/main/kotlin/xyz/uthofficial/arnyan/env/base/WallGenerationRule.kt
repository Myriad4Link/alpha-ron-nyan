package xyz.uthofficial.arnyan.env.base

import xyz.uthofficial.arnyan.env.tile.TileWall

fun interface WallGenerationRule {
    fun build(): TileWall
}