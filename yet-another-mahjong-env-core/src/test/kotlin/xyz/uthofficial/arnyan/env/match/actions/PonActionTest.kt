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
import xyz.uthofficial.arnyan.env.match.TestTileFactory
import xyz.uthofficial.arnyan.env.match.getPlayerBySeat
import xyz.uthofficial.arnyan.env.match.shouldBeSuccess
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH
import xyz.uthofficial.arnyan.env.wind.StandardWind.WEST

class PonActionTest : FunSpec({

    context("Pon.availableWhen") {
        test("should return false when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Man, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            Pon.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 5, false))
            match.submitDiscard(eastPlayer, Tile(Man, 5, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            Pon.availableWhen(match.observation, southPlayer, wrongTile) shouldBe false
        }

        test("should return false when actor is discarding player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))
            eastPlayer.closeHand.add(Tile(Man, 5, false))
            match.submitDiscard(eastPlayer, Tile(Man, 5, false)).shouldBeSuccess()

            val discardedTile = Tile(Man, 5, false)

            Pon.availableWhen(match.observation, eastPlayer, discardedTile) shouldBe false
        }

        test("should return false when no matching tiles") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Pin, 5, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 5, false))
            match.submitDiscard(eastPlayer, Tile(Man, 5, false)).shouldBeSuccess()

            val discardedTile = Tile(Man, 5, false)

            Pon.availableWhen(match.observation, southPlayer, discardedTile) shouldBe false
        }

        test("should return true for valid pon") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            eastPlayer.closeHand.clear()
            val man5 = Tile(Man, 5, false)
            eastPlayer.closeHand.add(man5)
            match.submitDiscard(eastPlayer, man5).shouldBeSuccess()

            Pon.availableWhen(match.observation, southPlayer, man5) shouldBe true
        }
    }

    context("Pon.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val actor = DummyPlayer()
            actor.seat = null
            val tile = Tile(Man, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, players[0])
            val obs = state.toObservation()

            val result = Pon.perform(obs, actor, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val southPlayer = getPlayerBySeat(players, SOUTH)
            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            val tile = Tile(Man, 5, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, southPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 5, false))
            match.submitDiscard(eastPlayer, Tile(Man, 5, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            val result = Pon.perform(match.observation, southPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when discarding player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val southPlayer = getPlayerBySeat(players, SOUTH)
            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            val discardingPlayer = DummyPlayer()
            discardingPlayer.seat = null
            val tile = Tile(Man, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, discardingPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when actor is discarding player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            val tile = Tile(Man, 5, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when no matching tiles") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Pin, 5, false)))

            eastPlayer.closeHand.clear()
            val tile = Tile(Man, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed with valid pon and update state correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            val man5a = Tile(Man, 5, false)
            val man5b = Tile(Man, 5, false)
            southPlayer.closeHand.addAll(listOf(man5a, man5b))

            eastPlayer.closeHand.clear()
            val man5c = Tile(Man, 5, false)
            eastPlayer.closeHand.add(man5c)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(man5c, eastPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, man5c).shouldBeSuccess()

            result.nextWind shouldBe SOUTH
            result.isOver shouldBe false
            result.observation.lastAction shouldBe LastAction.Pon(man5c, southPlayer)
        }

        test("should handle discards removal correctly when tile exists") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            eastPlayer.closeHand.clear()
            val man5 = Tile(Man, 5, false)
            eastPlayer.closeHand.add(man5)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(man5, Tile(Man, 1, false))
            state.lastAction = LastAction.Discard(man5, eastPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, man5).shouldBeSuccess()

            result.observation.discards[EAST]!! shouldNotContain man5
            result.observation.discards[EAST]!! shouldContain Tile(Man, 1, false)
        }

        test("should handle discards removal correctly when tile doesn't exist") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 5, false), Tile(Man, 5, false)))

            eastPlayer.closeHand.clear()
            val man5 = Tile(Man, 5, false)
            eastPlayer.closeHand.add(man5)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(Tile(Man, 1, false), Tile(Man, 2, false))
            state.lastAction = LastAction.Discard(man5, eastPlayer)
            val obs = state.toObservation()

            val result = Pon.perform(obs, southPlayer, man5).shouldBeSuccess()

            result.observation.discards[EAST] shouldBe listOf(Tile(Man, 1, false), Tile(Man, 2, false))
        }
    }
})

private fun Result<*, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? xyz.uthofficial.arnyan.env.result.Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as xyz.uthofficial.arnyan.env.result.Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}
