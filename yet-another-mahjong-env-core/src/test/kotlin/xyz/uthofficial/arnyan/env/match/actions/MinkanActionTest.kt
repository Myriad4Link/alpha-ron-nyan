package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.match.DummyPlayer
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchBuilder
import xyz.uthofficial.arnyan.env.match.StateChange
import xyz.uthofficial.arnyan.env.match.StepResult
import xyz.uthofficial.arnyan.env.match.TestTileFactory
import xyz.uthofficial.arnyan.env.match.getPlayerBySeat
import xyz.uthofficial.arnyan.env.match.shouldBeFailureWithActionNotAvailable
import xyz.uthofficial.arnyan.env.match.shouldBeSuccess
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.wind.StandardWind

class MinkanActionTest : FunSpec({

    test("Minkan.availableWhen should return false when last action is not Discard") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        MinKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Minkan.availableWhen should return false when discard tile doesn't match") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))

        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(2)) }

        val tile = TestTileFactory.createMan(2)

        MinKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Minkan.availableWhen should return false when actor is discarding player") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(4) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        MinKan.availableWhen(match.observation, eastPlayer, tile) shouldBe false
    }

    test("Minkan.availableWhen should return false when no three identical tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))

        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(2) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        MinKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Minkan.availableWhen should return true when three identical tiles available") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))

        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        MinKan.availableWhen(match.observation, southPlayer, tile) shouldBe true
    }

    test("Minkan.perform should fail when actor has no seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        val playerWithoutSeat = DummyPlayer()
        repeat(3) { playerWithoutSeat.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = MinKan.perform(obs, playerWithoutSeat, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should fail when last action is not Discard") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = MinKan.perform(obs, southPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should fail when tile doesn't match discard") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(2)) }

        val tile = TestTileFactory.createMan(2)
        val obs = match.observation

        val result = MinKan.perform(obs, southPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should fail when discarding player has no seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        
        val obsBefore = match.observation
        val modifiedState = obsBefore.toState()
        modifiedState.lastAction = LastAction.Discard(TestTileFactory.createMan(1), DummyPlayer())
        val modifiedObs = modifiedState.toObservation()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        val result = MinKan.perform(modifiedObs, southPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should fail when trying to minkan own discard") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(4) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        val obs = match.observation

        val result = MinKan.perform(obs, eastPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should fail when no three identical tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(2) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = MinKan.perform(obs, southPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Minkan.perform should succeed with valid minkan") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = MinKan.perform(obs, southPlayer, tile)
        result.shouldBeSuccess()

        val stepResult = result as Result.Success<StepResult>
        stepResult.value.observation.lastAction.shouldBeInstanceOf<LastAction.MinKan>()
        (stepResult.value.observation.lastAction as? LastAction.MinKan)?.let {
            it.tile shouldBe tile
            it.player shouldBe southPlayer
        }
        
        val stateChanges = stepResult.value.stateChanges
        stateChanges.any { it is StateChange.RemoveTilesFromHand } shouldBe true
        stateChanges.any { it is StateChange.AddOpenGroup } shouldBe true
        stateChanges.any { it is StateChange.RemoveTileFromDiscards } shouldBe true
        
        val addOpenGroupChange = stateChanges.find { it is StateChange.AddOpenGroup } as? StateChange.AddOpenGroup
        addOpenGroupChange?.group?.size shouldBe 4
    }

    test("Minkan.perform should handle empty discards list") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        
        val obsBefore = match.observation
        val modifiedState = obsBefore.toState()
        modifiedState.discards.clear()
        val modifiedObs = modifiedState.toObservation()

        val result = MinKan.perform(modifiedObs, southPlayer, tile)
        result.shouldBeSuccess()
    }

    test("Minkan.perform should remove tile from discards") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        eastPlayer.closeHand.add(TestTileFactory.createMan(1))
        match.submitDiscard(eastPlayer, TestTileFactory.createMan(1)).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = MinKan.perform(obs, southPlayer, tile)
        result.shouldBeSuccess()

        val stepResult = result as Result.Success<StepResult>
        val updatedDiscards = stepResult.value.observation.discards[StandardWind.EAST]
        updatedDiscards?.contains(tile) shouldBe false
    }

    test("findThreeIdenticalTiles should return three tiles when available") {
        val hand = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createPin(2)
        )
        val subject = TestTileFactory.createMan(1)

        val result = findThreeIdenticalTiles(hand, subject)
        result?.size shouldBe 3
        result?.all { it.tileType is xyz.uthofficial.arnyan.env.tile.Man && it.value == 1 } shouldBe true
    }

    test("findThreeIdenticalTiles should return null when less than three matching tiles") {
        val hand = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createPin(2)
        )
        val subject = TestTileFactory.createMan(1)

        val result = findThreeIdenticalTiles(hand, subject)
        result shouldBe null
    }
})
