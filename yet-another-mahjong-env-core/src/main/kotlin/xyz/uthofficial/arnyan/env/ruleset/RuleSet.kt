package xyz.uthofficial.arnyan.env.ruleset

import xyz.uthofficial.arnyan.env.base.WallGenerationRule
import xyz.uthofficial.arnyan.env.base.WindRotationRule
import xyz.uthofficial.arnyan.env.wind.StandardWind.*
import xyz.uthofficial.arnyan.env.wind.PlayerSeatWindRotationConfiguration
import xyz.uthofficial.arnyan.env.tile.TileSetConfiguration
import xyz.uthofficial.arnyan.env.tile.StandardTileType.*

data class RuleSet(
    val wallGenerationRule: WallGenerationRule,
    val playerWindRotationOrderRule: WindRotationRule
) {
    companion object {
        val RIICHI_SANMA_TENHOU = RuleSet(
            wallGenerationRule = {
                (TileSetConfiguration()
                    .setGroup {
                        allOf(SOU and PIN and WIND and DRAGON)
                        listOf(1, 9) of MAN
                    } repeatFor 4 whereEvery { SOU and PIN } has 1 redDoraOn 5)
                    .build()
            },
            playerWindRotationOrderRule = {
                PlayerSeatWindRotationConfiguration().apply {
                    EAST - SOUTH - WEST
                }.build()
            }
        )
    }
}