package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction

class RiichiIntegrationTest : FunSpec({

    test("RiichiAction should be integrated into MatchEngine action list") {
        val engine = MatchEngine()
        val allActions = engine.maskToActions(Int.MAX_VALUE)
        
        allActions shouldContain RiichiAction
    }

    test("RiichiAction ID should be correctly defined") {
        RiichiAction.id shouldBe 64  // 1 shl 6
    }

    test("RiichiAction toString should return RIICHI") {
        RiichiAction.toString() shouldBe "RIICHI"
    }

    test("LastAction.Riichi should be created correctly") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        
        val eastPlayer = players[0]
        eastPlayer.seat = EAST
        
        val tile = Tile(Sou, 5, false)
        val riichiAction = LastAction.Riichi(tile, eastPlayer)
        
        riichiAction.tile shouldBe tile
        riichiAction.player shouldBe eastPlayer
    }

    test("Riichi declaration should update player state") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = players[0] as DummyPlayer
        eastPlayer.score = 25000
        
        // Simple tenpai hand
        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.addAll(listOf(
            Tile(Sou, 1, false), Tile(Sou, 1, false), Tile(Sou, 1, false),
            Tile(Sou, 2, false), Tile(Sou, 2, false), Tile(Sou, 2, false),
            Tile(Sou, 3, false), Tile(Sou, 3, false), Tile(Sou, 3, false),
            Tile(Sou, 4, false), Tile(Sou, 4, false),
            Tile(Sou, 5, false), Tile(Sou, 6, false)
        ))
        
        val drawnTile = Tile(Sou, 7, false)
        eastPlayer.closeHand.add(drawnTile)
        
        val state = match.observation.toState()
        state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
        state.currentSeatWind = EAST
        state.riichiSticks = 0
        
        val obs = state.toObservation()
        val initialScore = eastPlayer.score
        
        val result = RiichiAction.perform(obs, eastPlayer, drawnTile)
        
        result.shouldBeInstanceOf<Result.Success<StepResult>>()
        val stepResult = (result as Result.Success).value
        
        // Verify state changes include score deduction and riichi stick
        val scoreChanges = stepResult.stateChanges.filterIsInstance<StateChange.UpdatePlayerScore>()
        scoreChanges.any { it.delta == -1000 } shouldBe true
        
        val riichiChanges = stepResult.stateChanges.filterIsInstance<StateChange.UpdateRiichiSticks>()
        riichiChanges.any { it.delta == 1 } shouldBe true
        
        // Verify last action is Riichi
        stepResult.observation.lastAction.shouldBeInstanceOf<LastAction.Riichi>()
        
        // Verify riichi sticks increased
        stepResult.observation.riichiSticks shouldBe 1
    }
})
