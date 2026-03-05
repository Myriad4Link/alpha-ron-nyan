package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import xyz.uthofficial.arnyan.env.tile.Dragon
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.Wind
import xyz.uthofficial.arnyan.env.wind.StandardWind

class NukiPeiActionTest : FunSpec({

    context("NukiPei.availableWhen") {
        test("should return false when actor is not current seat") {
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
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val tile = TestTileFactory.createMan(1)

            NukiPei.availableWhen(match.observation, southPlayer, tile) shouldBe false
        }

        test("should return false when last action is not Draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            val tile = TestTileFactory.createMan(1)
            match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            NukiPei.availableWhen(match.observation, eastPlayer, tile) shouldBe false
        }

        test("should return false when no North wind tile in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(13) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }

            val tile = TestTileFactory.createMan(1)

            NukiPei.availableWhen(match.observation, eastPlayer, tile) shouldBe false
        }

        test("should return false when wall has insufficient tiles") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val tile = TestTileFactory.createMan(1)

            NukiPei.availableWhen(match.observation, eastPlayer, tile) shouldBe false
        }
    }

    context("NukiPei.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val playerWithoutSeat = DummyPlayer()
            playerWithoutSeat.closeHand.add(TestTileFactory.createNorthWind())

            val tile = TestTileFactory.createMan(1)
            val obs = match.observation

            val result = NukiPei.perform(obs, playerWithoutSeat, tile)
            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not Draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)
            val southPlayer = getPlayerBySeat(players, StandardWind.SOUTH)

            val tile = TestTileFactory.createMan(1)
            match.submitDiscard(eastPlayer, tile).shouldBeSuccess()

            southPlayer.closeHand.clear()
            southPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val obs = match.observation

            val result = NukiPei.perform(obs, southPlayer, TestTileFactory.createMan(2))
            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when no North wind tile in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(13) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }

            val tile = TestTileFactory.createMan(1)
            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, tile)
            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should increment nukiCount on player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            eastPlayer.nukiCount shouldBe 0

            val tile = TestTileFactory.createMan(1)
            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, tile)
            result.shouldBeSuccess()

            eastPlayer.nukiCount shouldBe 1
        }
    }

    context("Nuki-Pei Integration Tests") {
        test("nuki increments nukiCount") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()

            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            eastPlayer.nukiCount shouldBe 0

            val obs = match.observation
            val northTile = eastPlayer.closeHand.find { it.tileType is Wind && it.value == 4 }!!
            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            eastPlayer.nukiCount shouldBe 1
        }
    }

    context("Nuki-Pei Edge Cases") {
        test("nuki works with riichi declared") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.isRiichiDeclared = true
            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val obs = match.observation
            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            eastPlayer.nukiCount shouldBe 1
        }
    }

    context("Nuki-Pei State Change Verification") {
        test("Verify RemoveTilesFromHand state change") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            val northTile = TestTileFactory.createNorthWind()
            eastPlayer.closeHand.add(northTile)

            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            val stepResult = result as Result.Success<StepResult>
            val removeChange = stepResult.value.stateChanges.find { it is StateChange.RemoveTilesFromHand } as? StateChange.RemoveTilesFromHand
            
            removeChange shouldNotBe null
            removeChange!!.tiles.size shouldBe 1
            removeChange.tiles.first() shouldBe northTile
        }

        test("Verify AddOpenGroup state change") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            val northTile = TestTileFactory.createNorthWind()
            eastPlayer.closeHand.add(northTile)

            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            val stepResult = result as Result.Success<StepResult>
            val addChange = stepResult.value.stateChanges.find { it is StateChange.AddOpenGroup } as? StateChange.AddOpenGroup
            
            addChange shouldNotBe null
            addChange!!.group.size shouldBe 1
            addChange.group.first() shouldBe northTile
        }

        test("Verify DrawReplacementTile state change") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            val stepResult = result as Result.Success<StepResult>
            val drawChange = stepResult.value.stateChanges.find { it is StateChange.DrawReplacementTile } as? StateChange.DrawReplacementTile
            
            drawChange shouldNotBe null
            drawChange!!.seat shouldBe StandardWind.EAST
        }

        test("Verify last action type is AnKan") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder()
                .withWallTiles(wallTiles)
                .withCustomPlayers(*players.toTypedArray())
                .build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, StandardWind.EAST)

            eastPlayer.closeHand.clear()
            repeat(12) { eastPlayer.closeHand.add(TestTileFactory.createMan((it % 9) + 1)) }
            eastPlayer.closeHand.add(TestTileFactory.createNorthWind())

            val obs = match.observation

            val result = NukiPei.perform(obs, eastPlayer, TestTileFactory.createMan(1))
            result.shouldBeSuccess()

            val stepResult = result as Result.Success<StepResult>
            val lastAction = stepResult.value.observation.lastAction
            
            lastAction.shouldBeInstanceOf<LastAction.AnKan>()
            val ankanAction = lastAction as LastAction.AnKan
            ankanAction.tile.tileType shouldBe Wind
            ankanAction.tile.value shouldBe 4
            ankanAction.player shouldBe eastPlayer
        }
    }
})
