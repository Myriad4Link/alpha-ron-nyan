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
import xyz.uthofficial.arnyan.env.match.TestTileFactory
import xyz.uthofficial.arnyan.env.match.getPlayerBySeat
import xyz.uthofficial.arnyan.env.match.shouldBeSuccess
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH

class PassActionTest : FunSpec({

    context("PassAction.availableWhen") {
        test("should return false when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            PassAction.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 1, false))
            match.submitDiscard(eastPlayer, Tile(Man, 1, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            PassAction.availableWhen(match.observation, southPlayer, wrongTile) shouldBe false
        }

        test("should return true when last action is discard and tile matches") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            eastPlayer.closeHand.clear()
            val man1 = Tile(Man, 1, false)
            eastPlayer.closeHand.add(man1)
            match.submitDiscard(eastPlayer, man1).shouldBeSuccess()

            PassAction.availableWhen(match.observation, southPlayer, man1) shouldBe true
        }
    }

    context("PassAction.perform") {
        test("should fail when player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val player = DummyPlayer()
            player.seat = null
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, players[0])
            val obs = state.toObservation()

            val result = PassAction.perform(obs, player, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Man, 1, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = PassAction.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 1, false))
            match.submitDiscard(eastPlayer, Tile(Man, 1, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            val result = PassAction.perform(match.observation, southPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed and return unchanged observation") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            eastPlayer.closeHand.clear()
            val man1 = Tile(Man, 1, false)
            eastPlayer.closeHand.add(man1)
            match.submitDiscard(eastPlayer, man1).shouldBeSuccess()

            val result = PassAction.perform(match.observation, southPlayer, man1).shouldBeSuccess()

            result.isOver shouldBe false
            result.nextWind shouldBe match.observation.currentSeatWind
            result.observation.discards shouldBe match.observation.discards
            result.stateChanges.shouldBeEmpty()
        }
    }
})

private fun Result<*, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}
