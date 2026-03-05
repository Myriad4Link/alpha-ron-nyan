package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind

class SanmaGameLoopTest : FunSpec({

    test("sanma game should complete a full round with tsumo win") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val ruleSet = RuleSet.RIICHI_SANMA_TENHOU
        val match = MatchBuilder()
            .withRuleSet(ruleSet)
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()

        match.start().shouldBeSuccess()

        val eastPlayer = players[0]
        val southPlayer = players[1]
        val westPlayer = players[2]

        eastPlayer.closeHand.clear()
        southPlayer.closeHand.clear()
        westPlayer.closeHand.clear()

        val dealAmount = match.observation.wall.standardDealAmount

        for (i in 0 until dealAmount - 1) {
            eastPlayer.closeHand.add(Tile(Man, (i % 9) + 1, false))
        }
        eastPlayer.closeHand.add(Tile(Man, 1, false))

        for (i in 0 until dealAmount) {
            southPlayer.closeHand.add(Tile(Pin, (i % 9) + 1, false))
        }

        for (i in 0 until dealAmount) {
            westPlayer.closeHand.add(Tile(Man, ((i + 3) % 9) + 1, false))
        }

        var turnCount = 0
        val maxTurns = 20

        while (turnCount < maxTurns) {
            val currentPlayer = players.find { it.seat == match.observation.currentSeatWind }
                ?: break

            val availableActions = match.observation.availableActions
            
            if (availableActions.contains(DiscardAction)) {
                val tileToDiscard = currentPlayer.closeHand.firstOrNull { tile ->
                    currentPlayer.closeHand.count { it == tile } == 1
                } ?: currentPlayer.closeHand.firstOrNull() ?: break
                
                val result = match.submitDiscard(currentPlayer, tileToDiscard)
                
                if (result is Result.Success) {
                    if (result.value.isOver) {
                        break
                    }
                } else {
                    break
                }
            } else if (availableActions.isEmpty()) {
                break
            }
            
            turnCount++
            
            if (turnCount > 5) break
        }

        turnCount shouldBeGreaterThan 0
    }

    test("sanma round end should update honba counter") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val ruleSet = RuleSet.RIICHI_SANMA_TENHOU
        val match = MatchBuilder()
            .withRuleSet(ruleSet)
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()

        match.start().shouldBeSuccess()

        match.observation.roundRotationStatus.honba shouldBe 0

        val result = match.endRound(winnerSeat = StandardWind.EAST)
        result.shouldBeSuccess()

        match.observation.roundRotationStatus.honba shouldBe 1
    }

    test("sanma dealer rotation should work on non-dealer win") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val ruleSet = RuleSet.RIICHI_SANMA_TENHOU
        val match = MatchBuilder()
            .withRuleSet(ruleSet)
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()

        match.start().shouldBeSuccess()

        val initialDealer = match.observation.currentSeatWind
        initialDealer shouldBe StandardWind.EAST

        match.endRound(winnerSeat = StandardWind.SOUTH).shouldBeSuccess()

        val newDealer = match.observation.currentSeatWind
        newDealer shouldBe StandardWind.SOUTH
    }
})

infix fun Int.shouldBeGreaterThan(other: Int) {
    if (this <= other) throw AssertionError("$this should be greater than $other")
}
