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
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.wind.StandardWind

class AnkanActionTest : FunSpec({

    test("Ankan.availableWhen should return false when actor is not current seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.closeHand.clear()
        repeat(4) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        AnKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Ankan.availableWhen should return false when last action is not Draw") {
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
        repeat(4) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(4) { southPlayer.closeHand.add(TestTileFactory.createMan(2)) }

        AnKan.availableWhen(match.observation, southPlayer, TestTileFactory.createMan(2)) shouldBe false
    }

    test("Ankan.availableWhen should return false when no four identical tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(2) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        AnKan.availableWhen(match.observation, eastPlayer, tile) shouldBe false
    }

    test("Ankan.availableWhen should return true when four identical tiles available") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(3) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)

        AnKan.availableWhen(match.observation, eastPlayer, tile) shouldBe true
    }

    test("Ankan.perform should fail when actor has no seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val playerWithoutSeat = DummyPlayer()
        repeat(3) { playerWithoutSeat.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = AnKan.perform(obs, playerWithoutSeat, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Ankan.perform should fail when last action is not Draw") {
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
        repeat(4) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        southPlayer.closeHand.clear()
        repeat(4) { southPlayer.closeHand.add(TestTileFactory.createMan(2)) }

        val obs = match.observation

        val result = AnKan.perform(obs, southPlayer, TestTileFactory.createMan(2))
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Ankan.perform should fail when no four identical tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(2) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = AnKan.perform(obs, eastPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Ankan.perform should succeed with valid ankan") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.closeHand.clear()
        repeat(3) { eastPlayer.closeHand.add(TestTileFactory.createMan(1)) }

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = AnKan.perform(obs, eastPlayer, tile)
        result.shouldBeSuccess()

        val stepResult = result as Result.Success<StepResult>
        stepResult.value.observation.lastAction.shouldBeInstanceOf<LastAction.AnKan>()
        (stepResult.value.observation.lastAction as? LastAction.AnKan)?.let {
            it.tile shouldBe tile
            it.player shouldBe eastPlayer
        }
        
        val stateChanges = stepResult.value.stateChanges
        stateChanges.any { it is StateChange.RemoveTilesFromHand } shouldBe true
        stateChanges.any { it is StateChange.AddOpenGroup } shouldBe true
        
        val addOpenGroupChange = stateChanges.find { it is StateChange.AddOpenGroup } as? StateChange.AddOpenGroup
        addOpenGroupChange?.group?.size shouldBe 4
    }

    test("findFourIdenticalTiles should return four tiles when available") {
        val hand = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createPin(2)
        )
        val subject = TestTileFactory.createMan(1)

        val result = findFourIdenticalTiles(hand, subject)
        result?.size shouldBe 4
        result?.all { it.tileType is Man && it.value == 1 } shouldBe true
    }

    test("findFourIdenticalTiles should return null when less than three matching tiles") {
        val hand = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createPin(2)
        )
        val subject = TestTileFactory.createMan(1)

        val result = findFourIdenticalTiles(hand, subject)
        result shouldBe null
    }
})
