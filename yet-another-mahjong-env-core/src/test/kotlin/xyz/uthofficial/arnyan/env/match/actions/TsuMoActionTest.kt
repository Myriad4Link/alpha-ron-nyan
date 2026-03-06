package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.match.DummyPlayer
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchBuilder
import xyz.uthofficial.arnyan.env.match.StateChange
import xyz.uthofficial.arnyan.env.match.TestTileFactory
import xyz.uthofficial.arnyan.env.match.getPlayerBySeat
import xyz.uthofficial.arnyan.env.match.shouldBeSuccess
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH

class TsuMoActionTest : FunSpec({

    context("TsuMo.availableWhen") {
        test("should return false when last action is not draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when tile doesn't match drawn tile") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val drawnTile = Tile(Man, 1, false)
            val wrongTile = Tile(Man, 9, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, wrongTile) shouldBe false
        }

        test("should return false when last action is not actor's draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, southPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when hand is incomplete") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))
            val tile = Tile(Man, 3, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when hand has no yaku") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.openHand.clear()
            eastPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            eastPlayer.openHand.add(listOf(Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false)))
            eastPlayer.openHand.add(listOf(Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false)))
            eastPlayer.closeHand.addAll(listOf(
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))
            val tile = Tile(Pin, 4, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return true for valid tsumo") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }
            val tile = Tile(Pin, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe true
        }
    }

    context("TsuMo.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val actor = DummyPlayer()
            actor.seat = null
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, players[0])
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, actor, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }

            val tile = Tile(Pin, 2, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when tile doesn't match drawn tile") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }

            val drawnTile = Tile(Pin, 2, false)
            val wrongTile = Tile(Man, 9, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not actor's draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }

            val tile = Tile(Pin, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, southPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when hand is incomplete") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))
            val tile = Tile(Man, 3, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when hand has no yaku") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.openHand.clear()
            eastPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            eastPlayer.openHand.add(listOf(Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false)))
            eastPlayer.openHand.add(listOf(Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false)))
            eastPlayer.closeHand.addAll(listOf(
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))
            val tile = Tile(Pin, 4, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed with valid tsumo and update state correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }
            val winningTile = Tile(Pin, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, winningTile).shouldBeSuccess()

            result.isOver shouldBe true
            result.nextWind shouldBe EAST
            result.observation.lastAction shouldBe LastAction.TsuMo(winningTile, eastPlayer)
        }

        test("should compute scoring state changes correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.score = 50000
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 1, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 2, false)) }
            val winningTile = Tile(Pin, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(winningTile, eastPlayer)
            state.riichiSticks = 1
            state.honbaSticks = 2
            val obs = state.toObservation()

            val result = TsuMo.perform(obs, eastPlayer, winningTile).shouldBeSuccess()

            val scoreChanges = result.stateChanges.filterIsInstance<StateChange.UpdatePlayerScore>()
            scoreChanges.any { it.delta > 0 } shouldBe true
        }
    }
})

private fun Result<*, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}
