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
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.match.DummyPlayer
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchBuilder
import xyz.uthofficial.arnyan.env.match.MatchState
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
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH
import xyz.uthofficial.arnyan.env.wind.StandardWind.WEST

class RonActionTest : FunSpec({

    context("Ron.availableWhen") {
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

            Ron.availableWhen(obs, eastPlayer, tile) shouldBe false
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

            Ron.availableWhen(match.observation, southPlayer, wrongTile) shouldBe false
        }

        test("should return false when actor is discarding player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 1, false))
            match.submitDiscard(eastPlayer, Tile(Man, 1, false)).shouldBeSuccess()

            val discardedTile = Tile(Man, 1, false)

            Ron.availableWhen(match.observation, eastPlayer, discardedTile) shouldBe false
        }

        test("should return false when hand is incomplete") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 3, false))
            match.submitDiscard(eastPlayer, Tile(Man, 3, false)).shouldBeSuccess()

            val discardedTile = Tile(Man, 3, false)

            Ron.availableWhen(match.observation, southPlayer, discardedTile) shouldBe false
        }

        test("should return false when hand has no yaku") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.openHand.clear()
            southPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            southPlayer.openHand.add(listOf(Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false)))
            southPlayer.openHand.add(listOf(Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false)))
            southPlayer.closeHand.addAll(listOf(
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Pin, 4, false))
            match.submitDiscard(eastPlayer, Tile(Pin, 4, false)).shouldBeSuccess()

            val discardedTile = Tile(Pin, 4, false)

            Ron.availableWhen(match.observation, southPlayer, discardedTile) shouldBe false
        }

        test("should return true for valid winning hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 5, false)
            eastPlayer.closeHand.add(winningTile)
            match.submitDiscard(eastPlayer, winningTile).shouldBeSuccess()

            Ron.availableWhen(match.observation, southPlayer, winningTile) shouldBe true
        }
    }

    context("Ron.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val actor = DummyPlayer()
            actor.seat = null

            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, actor, winningTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val tile = Tile(Pin, 5, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, eastPlayer, tile)

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
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 1, false))
            match.submitDiscard(eastPlayer, Tile(Man, 1, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            val result = Ron.perform(match.observation, southPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when discarding player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val discardingPlayer = DummyPlayer()
            discardingPlayer.seat = null
            val winningTile = Tile(Pin, 5, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(winningTile, discardingPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when actor is discarding player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)

            eastPlayer.closeHand.clear()
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { eastPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { eastPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, eastPlayer, winningTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when hand is incomplete") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))

            eastPlayer.closeHand.clear()
            val tile = Tile(Man, 3, false)
            eastPlayer.closeHand.add(tile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when hand has no yaku") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.openHand.clear()
            southPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            southPlayer.openHand.add(listOf(Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false)))
            southPlayer.openHand.add(listOf(Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false)))
            southPlayer.closeHand.addAll(listOf(
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 4, false)
            eastPlayer.closeHand.add(winningTile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed with valid ron and update state correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 5, false)
            eastPlayer.closeHand.add(winningTile)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile).shouldBeSuccess()

            result.isOver shouldBe true
            result.nextWind shouldBe SOUTH
            result.observation.lastAction shouldBe LastAction.Ron(winningTile, southPlayer)
        }

        test("should handle discards removal correctly when tile exists") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 5, false)
            eastPlayer.closeHand.add(winningTile)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(winningTile, Tile(Man, 1, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile).shouldBeSuccess()

            result.observation.discards[EAST]!! shouldNotContain winningTile
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
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 5, false)
            eastPlayer.closeHand.add(winningTile)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(Tile(Man, 1, false), Tile(Man, 2, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile).shouldBeSuccess()

            result.observation.discards[EAST] shouldBe listOf(Tile(Man, 1, false), Tile(Man, 2, false))
        }

        test("should compute scoring state changes with riichi and honba sticks") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.score = 50000
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            eastPlayer.closeHand.clear()
            val winningTile = Tile(Pin, 5, false)
            eastPlayer.closeHand.add(winningTile)

            val state = match.observation.toState()
            state.riichiSticks = 2
            state.honbaSticks = 3
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            val obs = state.toObservation()

            val result = Ron.perform(obs, southPlayer, winningTile).shouldBeSuccess()

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
