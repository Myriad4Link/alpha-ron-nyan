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

class ChiiActionTest : FunSpec({

    context("Chii.availableWhen") {
        test("should return false when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val tile = Tile(Man, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, eastPlayer)
            val obs = state.toObservation()

            Chii.availableWhen(obs, eastPlayer, tile) shouldBe false
        }

        test("should return false when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            Chii.availableWhen(match.observation, westPlayer, wrongTile) shouldBe false
        }

        test("should return false when discarding player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val discardingPlayer = DummyPlayer()
            discardingPlayer.seat = null
            val tile = Tile(Man, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, discardingPlayer)
            val obs = state.toObservation()

            Chii.availableWhen(obs, players[0], tile) shouldBe false
        }

        test("should return false when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val actor = DummyPlayer()
            actor.seat = null

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            Chii.availableWhen(match.observation, actor, Tile(Man, 2, false)) shouldBe false
        }

        test("should return false when topology getKamicha fails") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            Chii.availableWhen(match.observation, southPlayer, Tile(Man, 2, false)) shouldBe false
        }

        test("should return false when actor is not kamicha") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            Chii.availableWhen(match.observation, southPlayer, Tile(Man, 2, false)) shouldBe false
        }

        test("should return false when no sequence tiles available") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Pin, 1, false), Tile(Pin, 9, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            Chii.availableWhen(match.observation, westPlayer, Tile(Man, 2, false)) shouldBe false
        }

        test("should return true for valid chii") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            Chii.availableWhen(match.observation, westPlayer, Tile(Man, 2, false)) shouldBe true
        }
    }

    context("Chii.perform") {
        test("should fail when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val actor = DummyPlayer()
            actor.seat = null

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(Tile(Man, 2, false), eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, actor, Tile(Man, 2, false))

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when last action is not discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val westPlayer = getPlayerBySeat(players, WEST)
            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            val tile = Tile(Man, 2, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Draw(tile, westPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when tile doesn't match discard") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))
            match.submitDiscard(eastPlayer, Tile(Man, 2, false)).shouldBeSuccess()

            val wrongTile = Tile(Man, 9, false)

            val result = Chii.perform(match.observation, westPlayer, wrongTile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when discarding player has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val westPlayer = getPlayerBySeat(players, WEST)
            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            val discardingPlayer = DummyPlayer()
            discardingPlayer.seat = null
            val tile = Tile(Man, 2, false)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, discardingPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when actor is discarding player") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            val tile = Tile(Man, 2, false)
            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(tile, eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, eastPlayer, tile)

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when actor is not kamicha") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            southPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(Tile(Man, 2, false), eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, southPlayer, Tile(Man, 2, false))

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should fail when no sequence tiles available") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Pin, 1, false), Tile(Pin, 9, false)))

            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.add(Tile(Man, 2, false))

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(Tile(Man, 2, false), eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, Tile(Man, 2, false))

            result.shouldBeFailureWithActionNotAvailable()
        }

        test("should succeed with valid chii and update state correctly") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            val man1 = Tile(Man, 1, false)
            val man3 = Tile(Man, 3, false)
            westPlayer.closeHand.addAll(listOf(man1, man3))

            eastPlayer.closeHand.clear()
            val man2 = Tile(Man, 2, false)
            eastPlayer.closeHand.add(man2)

            val state = match.observation.toState()
            state.lastAction = LastAction.Discard(man2, eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, man2).shouldBeSuccess()

            result.nextWind shouldBe WEST
            result.isOver shouldBe false
            result.observation.lastAction shouldBe LastAction.Chii(man2, westPlayer)
        }

        test("should handle discards removal correctly when tile exists") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            val man2 = Tile(Man, 2, false)
            eastPlayer.closeHand.add(man2)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(man2, Tile(Man, 5, false))
            state.lastAction = LastAction.Discard(man2, eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, man2).shouldBeSuccess()

            result.observation.discards[EAST]!! shouldNotContain man2
            result.observation.discards[EAST]!! shouldContain Tile(Man, 5, false)
        }

        test("should handle discards removal correctly when tile doesn't exist") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val westPlayer = getPlayerBySeat(players, WEST)

            westPlayer.closeHand.clear()
            westPlayer.closeHand.addAll(listOf(Tile(Man, 1, false), Tile(Man, 3, false)))

            eastPlayer.closeHand.clear()
            val man2 = Tile(Man, 2, false)
            eastPlayer.closeHand.add(man2)

            val state = match.observation.toState()
            state.discards[EAST] = mutableListOf(Tile(Man, 5, false), Tile(Man, 6, false))
            state.lastAction = LastAction.Discard(man2, eastPlayer)
            val obs = state.toObservation()

            val result = Chii.perform(obs, westPlayer, man2).shouldBeSuccess()

            result.observation.discards[EAST] shouldBe listOf(Tile(Man, 5, false), Tile(Man, 6, false))
        }
    }
})

private fun Result<*, ActionError>.shouldBeFailureWithActionNotAvailable() {
    (this as? Result.Failure<ActionError>) shouldNotBe null
    val actionError = (this as Result.Failure).error
    (actionError as? ActionError.Match) shouldNotBe null
    ((actionError as ActionError.Match).error as? MatchError.ActionNotAvailable) shouldNotBe null
}
