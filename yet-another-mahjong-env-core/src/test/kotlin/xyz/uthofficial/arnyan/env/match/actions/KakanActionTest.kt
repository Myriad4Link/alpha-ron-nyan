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
import xyz.uthofficial.arnyan.env.wind.StandardWind

class KakanActionTest : FunSpec({

    test("Kakan.availableWhen should return false when actor is not current seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))

        val tile = TestTileFactory.createMan(1)

        KaKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Kakan.availableWhen should return false when last action is not Draw") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        eastPlayer.openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        KaKan.availableWhen(match.observation, southPlayer, tile) shouldBe false
    }

    test("Kakan.availableWhen should return false when no existing pon") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.openHand.add(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2)
        ))

        val tile = TestTileFactory.createMan(1)

        KaKan.availableWhen(match.observation, eastPlayer, tile) shouldBe false
    }

    test("Kakan.availableWhen should return true when existing pon available") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))

        val tile = TestTileFactory.createMan(1)

        KaKan.availableWhen(match.observation, eastPlayer, tile) shouldBe true
    }

    test("Kakan.perform should fail when actor has no seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val playerWithoutSeat = DummyPlayer()
        playerWithoutSeat.openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = KaKan.perform(obs, playerWithoutSeat, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Kakan.perform should fail when last action is not Draw") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
        val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

        southPlayer.openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))

        val tile = TestTileFactory.createMan(1)
        match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

        val obs = match.observation

        val result = KaKan.perform(obs, southPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Kakan.perform should fail when no existing pon") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        eastPlayer.openHand.add(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2)
        ))

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = KaKan.perform(obs, eastPlayer, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("Kakan.perform should succeed with valid kakan") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder()
            .withWallTiles(wallTiles)
            .withCustomPlayers(*players.toTypedArray())
            .build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

        val ponTiles = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        )
        eastPlayer.openHand.add(ponTiles)

        val tile = TestTileFactory.createMan(1)
        val obs = match.observation

        val result = KaKan.perform(obs, eastPlayer, tile)
        result.shouldBeSuccess()

        val stepResult = result as Result.Success<StepResult>
        stepResult.value.observation.lastAction.shouldBeInstanceOf<LastAction.KaKan>()
        (stepResult.value.observation.lastAction as? LastAction.KaKan)?.let {
            it.tile shouldBe tile
            it.player shouldBe eastPlayer
        }
        
        val stateChanges = stepResult.value.stateChanges
        stateChanges.any { it is StateChange.RemoveOpenGroup } shouldBe true
        stateChanges.any { it is StateChange.AddOpenGroup } shouldBe true
        
        val addOpenGroupChange = stateChanges.find { it is StateChange.AddOpenGroup } as? StateChange.AddOpenGroup
        addOpenGroupChange?.group?.size shouldBe 4
    }

    test("findExistingPon should return pon group when exists") {
        val openHand = listOf(
            listOf(
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1)
            ),
            listOf(
                TestTileFactory.createPin(2),
                TestTileFactory.createPin(2),
                TestTileFactory.createPin(2)
            )
        )
        val subject = TestTileFactory.createMan(1)

        val result = findExistingPon(openHand, subject)
        result?.size shouldBe 3
        result?.all { it.tileType is xyz.uthofficial.arnyan.env.tile.Man && it.value == 1 } shouldBe true
    }

    test("findExistingPon should return null when no matching pon") {
        val openHand = listOf(
            listOf(
                TestTileFactory.createMan(2),
                TestTileFactory.createMan(2),
                TestTileFactory.createMan(2)
            )
        )
        val subject = TestTileFactory.createMan(1)

        val result = findExistingPon(openHand, subject)
        result shouldBe null
    }

    test("findExistingPon should return null when group size is not 3") {
        val openHand = listOf(
            listOf(
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1)
            )
        )
        val subject = TestTileFactory.createMan(1)

        val result = findExistingPon(openHand, subject)
        result shouldBe null
    }

    test("findExistingPonIndex should return index when exists") {
        val openHand = listOf(
            listOf(
                TestTileFactory.createPin(2),
                TestTileFactory.createPin(2),
                TestTileFactory.createPin(2)
            ),
            listOf(
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1),
                TestTileFactory.createMan(1)
            )
        )
        val subject = TestTileFactory.createMan(1)

        val result = findExistingPonIndex(openHand, subject)
        result shouldBe 1
    }

    test("findExistingPonIndex should return null when not found") {
        val openHand = listOf(
            listOf(
                TestTileFactory.createMan(2),
                TestTileFactory.createMan(2),
                TestTileFactory.createMan(2)
            )
        )
        val subject = TestTileFactory.createMan(1)

        val result = findExistingPonIndex(openHand, subject)
        result shouldBe null
    }
})
