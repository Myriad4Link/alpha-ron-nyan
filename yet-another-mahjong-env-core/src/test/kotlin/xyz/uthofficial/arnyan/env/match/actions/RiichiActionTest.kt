package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
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

class RiichiActionTest : FunSpec({

    context("RiichiAction.availableWhen") {
        test("should return false when last action is not draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when last action is not actor's draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, southPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when tile doesn't match drawn tile") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val drawnTile = Tile(Sou, 5, false)
            val wrongTile = Tile(Sou, 9, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, wrongTile) shouldBe false
        }

        test("should return false when not actor's turn") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            state.currentSeatWind = SOUTH
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, southPlayer, tile) shouldBe false
        }

        test("should return false with open hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.openHand.clear()
            eastPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when already declared") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.isRiichiDeclared = true
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false with insufficient score") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 500
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when not in tenpai") {
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
            val tile = Tile(Sou, 5, false)
            eastPlayer.closeHand.add(tile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return true for valid riichi declaration") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Sou, 1, false), Tile(Sou, 1, false), Tile(Sou, 1, false),
                Tile(Sou, 2, false), Tile(Sou, 2, false), Tile(Sou, 2, false),
                Tile(Sou, 3, false), Tile(Sou, 3, false), Tile(Sou, 3, false),
                Tile(Sou, 4, false), Tile(Sou, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 6, false)
            ))
            val drawnTile = Tile(Sou, 9, false)
            eastPlayer.closeHand.add(drawnTile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, drawnTile) shouldBe true
        }

        test("should return true when riichi is possible by discarding a different tile than drawn") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Man, 3, false), Tile(Man, 3, false), Tile(Man, 3, false),
                Tile(Man, 4, false), Tile(Man, 4, false),
                Tile(Man, 5, false),
                Tile(Sou, 9, false)
            ))
            val drawnTile = Tile(Man, 5, false)
            eastPlayer.closeHand.add(drawnTile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, drawnTile) shouldBe true

            val discardTile = Tile(Sou, 9, false)
            RiichiAction.availableWhen(obs, eastPlayer, discardTile) shouldBe true
        }

        test("should return false when no discard candidate achieves tenpai") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Man, 3, false), Tile(Man, 3, false), Tile(Man, 3, false),
                Tile(Man, 4, false), Tile(Man, 4, false),
                Tile(Man, 5, false),
                Tile(Sou, 9, false)
            ))
            val drawnTile = Tile(Sou, 5, false)
            eastPlayer.closeHand.add(drawnTile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            RiichiAction.availableWhen(obs, eastPlayer, drawnTile) shouldBe false
            RiichiAction.availableWhen(obs, eastPlayer, Tile(Sou, 9, false)) shouldBe false
        }
    }

    context("RiichiAction.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val actor = DummyPlayer()
            actor.seat = null
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, players[0])
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, actor, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not actor's draw") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)
            eastPlayer.score = 25000
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, southPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail with open hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.openHand.clear()
            eastPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when already declared") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.isRiichiDeclared = true
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail with insufficient score") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 500
            val tile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when not in tenpai") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))
            val tile = Tile(Sou, 5, false)
            eastPlayer.closeHand.add(tile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed and update state correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
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
            state.riichiSticks = 0
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, drawnTile).shouldBeSuccess()

            result.observation.lastAction shouldBe LastAction.Riichi(drawnTile, eastPlayer)
            result.observation.riichiSticks shouldBe 1
            result.nextWind shouldBe SOUTH
            result.isOver shouldBe false
        }

        test("should handle score and riichi stick changes correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
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
            state.riichiSticks = 0
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, drawnTile).shouldBeSuccess()

            val scoreChanges = result.stateChanges.filterIsInstance<StateChange.UpdatePlayerScore>()
            scoreChanges.any { it.delta == -1000 } shouldBe true

            val riichiChanges = result.stateChanges.filterIsInstance<StateChange.UpdateRiichiSticks>()
            riichiChanges.any { it.delta == 1 } shouldBe true
        }

        test("should handle discards addition correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
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
            state.discards[EAST] = mutableListOf(Tile(Man, 1, false))
            state.riichiSticks = 0
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, drawnTile).shouldBeSuccess()

            result.observation.discards[EAST]!! shouldContain drawnTile
        }

        test("should succeed when discarding a tile other than the drawn tile") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Man, 3, false), Tile(Man, 3, false), Tile(Man, 3, false),
                Tile(Man, 4, false), Tile(Man, 4, false),
                Tile(Man, 5, false),
                Tile(Sou, 9, false)
            ))
            val drawnTile = Tile(Man, 5, false)
            eastPlayer.closeHand.add(drawnTile)
            val discardTile = Tile(Sou, 9, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            state.riichiSticks = 0
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, discardTile).shouldBeSuccess()

            result.observation.lastAction shouldBe LastAction.Riichi(discardTile, eastPlayer)
            result.observation.discards[EAST]!! shouldContain discardTile
            
            val removeChanges = result.stateChanges.filterIsInstance<StateChange.RemoveTilesFromHand>()
            removeChanges.any { it.seat == EAST && it.tiles.contains(discardTile) } shouldBe true
        }

        test("should fail when discard tile is not in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.score = 25000
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Man, 3, false), Tile(Man, 3, false), Tile(Man, 3, false),
                Tile(Man, 4, false), Tile(Man, 4, false),
                Tile(Man, 5, false),
                Tile(Sou, 9, false)
            ))
            val drawnTile = Tile(Man, 5, false)
            eastPlayer.closeHand.add(drawnTile)
            val wrongTile = Tile(Pin, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(drawnTile, eastPlayer)
            val obs = state.toObservation()

            val result = RiichiAction.perform(obs, eastPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }
    }
})

private fun Result<*, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}
