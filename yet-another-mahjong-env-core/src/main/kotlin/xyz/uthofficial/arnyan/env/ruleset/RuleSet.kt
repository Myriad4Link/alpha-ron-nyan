package xyz.uthofficial.arnyan.env.ruleset

import xyz.uthofficial.arnyan.env.ruleset.base.WallGenerationRule
import xyz.uthofficial.arnyan.env.ruleset.base.WindRotationRule
import xyz.uthofficial.arnyan.env.wind.StandardWind.*
import xyz.uthofficial.arnyan.env.wind.PlayerSeatWindRotationConfiguration
import xyz.uthofficial.arnyan.env.tile.TileSetConfiguration
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Wind
import xyz.uthofficial.arnyan.env.tile.Dragon
import xyz.uthofficial.arnyan.env.tile.dsl.*

data class RuleSet(
    val wallGenerationRule: WallGenerationRule,
    val playerWindRotationOrderRule: WindRotationRule
) {
    companion object {
        val RIICHI_SANMA_TENHOU = RuleSet(
            wallGenerationRule = {
                (TileSetConfiguration()
                    .setGroup {
                        allOf(Sou and Pin and Wind and Dragon) + (listOf(1, 9) of Man)
                    } repeatFor 4 whereEvery { Sou and Pin } has 1 redDoraOn 5)
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