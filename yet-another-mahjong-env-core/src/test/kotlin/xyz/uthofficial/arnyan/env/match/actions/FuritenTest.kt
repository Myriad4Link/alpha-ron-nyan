package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.match.DummyPlayer
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchBuilder
import xyz.uthofficial.arnyan.env.match.TestTileFactory
import xyz.uthofficial.arnyan.env.match.getPlayerBySeat
import xyz.uthofficial.arnyan.env.match.shouldBeSuccess
import xyz.uthofficial.arnyan.env.match.toState
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.wind.StandardWind.SOUTH

class FuritenTest : FunSpec({

    context("Standard Furiten - tile in own discards") {
        test("should not allow ron when winning tile is in own discards") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            val state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(winningTile, Tile(Pin, 6, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            
            val obs = state.toObservation()

            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe false
        }

        test("should allow ron when winning tile is not in own discards") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            val state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false), Tile(Pin, 7, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            
            val obs = state.toObservation()

            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe true
        }
    }

    context("Temporary Furiten - passing on win") {
        test("should enter temporary furiten after passing on ron") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            var state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            
            var obs = state.toObservation()
            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe true

            val passResult = PassAction.perform(obs, southPlayer, winningTile).shouldBeSuccess()
            
            passResult.observation.temporaryFuritenPlayers shouldContain SOUTH
            
            obs = passResult.observation
            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe false
        }

        test("should clear temporary furiten after drawing tile") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }

            val winningTile = Tile(Pin, 5, false)
            var state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            
            var obs = state.toObservation()
            
            val passResult = PassAction.perform(obs, southPlayer, winningTile).shouldBeSuccess()
            Ron.availableWhen(passResult.observation, southPlayer, winningTile) shouldBe false
            
            state = passResult.observation.toState()
            state.temporaryFuritenPlayers shouldContain SOUTH
            
            state.lastAction = LastAction.Draw(Tile(Pin, 1, false), southPlayer)
            state.currentSeatWind = SOUTH
            state.temporaryFuritenPlayers.remove(SOUTH)
            
            obs = state.toObservation()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false), Tile(Pin, 7, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            obs = state.toObservation()
            
            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe true
        }
    }

    context("Permanent Furiten (Riichi Furiten)") {
        test("should enter permanent furiten after riichi and passing on win") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }
            southPlayer.isRiichiDeclared = true

            val winningTile = Tile(Pin, 5, false)
            var state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false), Tile(Pin, 7, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            
            var obs = state.toObservation()
            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe true

            val passResult = PassAction.perform(obs, southPlayer, winningTile).shouldBeSuccess()
            
            passResult.observation.furitenPlayers shouldContain SOUTH
            
            obs = passResult.observation
            Ron.availableWhen(obs, southPlayer, winningTile) shouldBe false
            
            state = obs.toState()
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false), Tile(Pin, 7, false), Tile(Pin, 8, false))
            state.lastAction = LastAction.Discard(Tile(Man, 7, false), eastPlayer)
            obs = state.toObservation()
            
            Ron.availableWhen(obs, southPlayer, Tile(Man, 7, false)) shouldBe false
        }

        test("permanent furiten persists until round end") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            val southPlayer = getPlayerBySeat(players, SOUTH)

            southPlayer.closeHand.clear()
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 2, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Man, 3, false)) }
            repeat(3) { southPlayer.closeHand.add(Tile(Pin, 4, false)) }
            repeat(2) { southPlayer.closeHand.add(Tile(Pin, 5, false)) }
            southPlayer.isRiichiDeclared = true

            val winningTile = Tile(Pin, 5, false)
            var state = match.observation.toState()
            
            state.discards[SOUTH] = mutableListOf(Tile(Pin, 6, false))
            state.lastAction = LastAction.Discard(winningTile, eastPlayer)
            state.furitenPlayers.add(SOUTH)
            
            var obs = state.toObservation()
            obs.furitenPlayers shouldContain SOUTH

            match.endRound(winnerSeat = EAST).shouldBeSuccess()
            match.startNextRound().shouldBeSuccess()

            obs = match.observation
            obs.furitenPlayers shouldBe emptySet()
            obs.temporaryFuritenPlayers shouldBe emptySet()
        }
    }
})
