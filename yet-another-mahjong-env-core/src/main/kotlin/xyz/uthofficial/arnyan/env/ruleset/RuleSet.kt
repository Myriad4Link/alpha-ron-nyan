package xyz.uthofficial.arnyan.env.ruleset

import xyz.uthofficial.arnyan.env.ruleset.base.PlayerWindRotationRule
import xyz.uthofficial.arnyan.env.ruleset.base.RoundWindRotationRule
import xyz.uthofficial.arnyan.env.ruleset.base.WallGenerationRule
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.tile.dsl.allOf
import xyz.uthofficial.arnyan.env.tile.dsl.and
import xyz.uthofficial.arnyan.env.tile.dsl.of
import xyz.uthofficial.arnyan.env.wind.PlayerSeatWindRotationConfiguration
import xyz.uthofficial.arnyan.env.wind.RoundWindRotationConfiguration
import xyz.uthofficial.arnyan.env.wind.StandardWind.*

data class RuleSet(
    val wallGenerationRule: WallGenerationRule,
    val playerWindRotationOrderRule: PlayerWindRotationRule,
    val roundWindRotationRule: RoundWindRotationRule = {
        RoundWindRotationConfiguration().apply {
            EAST * 4
            SOUTH * 4
        }.build()
    }
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
            },
            roundWindRotationRule = {
                RoundWindRotationConfiguration().apply {
                    EAST * 4
                    SOUTH * 4
                }.build()
            }
        )
    }
}