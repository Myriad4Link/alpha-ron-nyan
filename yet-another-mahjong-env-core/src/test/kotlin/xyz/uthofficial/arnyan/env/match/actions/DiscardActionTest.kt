package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
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
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH

/**
 * Test file for DiscardAction.
 * Note: Most DiscardAction tests are integration tests in MatchTest.kt
 * since discarding is tightly coupled with match flow.
 */
class DiscardActionTest : FunSpec({

    context("DiscardAction.availableWhen") {
        test("should return true only for current player with tile in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = eastPlayer.closeHand.first()

            DiscardAction.availableWhen(obs, eastPlayer, tile) shouldBe true
        }

        test("should return false for wrong player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val southPlayer = getPlayerBySeat(players, SOUTH)
            val tile = TestTileFactory.createMan(1)

            DiscardAction.availableWhen(obs, southPlayer, tile) shouldBe false
        }

        test("should return false when tile not in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val eastPlayer = getPlayerBySeat(players, EAST)
            val tileNotInHand = TestTileFactory.createMan(9)

            DiscardAction.availableWhen(obs, eastPlayer, tileNotInHand) shouldBe false
        }
    }

    context("DiscardAction.perform") {
        test("should move tile to discards and advance turn") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = eastPlayer.closeHand.first()

            val result = DiscardAction.perform(obs, eastPlayer, tile).shouldBeSuccess()

            result.observation.currentSeatWind shouldBe EAST
            result.observation.discards[EAST] shouldNotBe null
            result.observation.discards[EAST]!! shouldContain tile
            result.nextWind shouldBe EAST
            result.observation.lastAction shouldBe LastAction.Discard(tile, eastPlayer)
        }

        test("should fail when not player's turn") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val southPlayer = getPlayerBySeat(players, SOUTH)
            val tile = TestTileFactory.createMan(1)

            val result = DiscardAction.perform(obs, southPlayer, tile)
            result.shouldBeFailureWithNotPlayersTurn()
        }

        test("should fail when tile not in hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val eastPlayer = getPlayerBySeat(players, EAST)
            val tileNotInHand = TestTileFactory.createMan(9)

            val result = DiscardAction.perform(obs, eastPlayer, tileNotInHand)
            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val obs = match.observation
            val playerWithoutSeat = DummyPlayer()
            val tile = TestTileFactory.createMan(1)

            val result = DiscardAction.perform(obs, playerWithoutSeat, tile)
            result.shouldBeFailureWithActionNotAvailable()
        }
    }
})

private fun <T> Result<T, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}

private fun <T> Result<T, ActionError>.shouldBeFailureWithNotPlayersTurn() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.NotPlayersTurn) shouldNotBe null
}
