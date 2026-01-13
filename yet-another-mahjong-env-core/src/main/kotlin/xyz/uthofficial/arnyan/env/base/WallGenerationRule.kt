package xyz.uthofficial.arnyan.env.base

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.TileWall

fun interface WallGenerationRule {
    fun build(): Result<TileWall, ConfigurationError>
}