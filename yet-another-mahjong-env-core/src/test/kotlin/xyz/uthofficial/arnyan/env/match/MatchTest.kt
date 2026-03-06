package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardWind.*
import xyz.uthofficial.arnyan.env.match.actions.AnKan
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.KaKan
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo

class MatchTest : FunSpec({

    fun getPlayerBySeat(players: List<DummyPlayer>, seat: StandardWind): DummyPlayer =
        players.find { it.seat == seat }!!

    test("Match creation should initialize with correct state") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()

        val obs = match.observation
        obs.players shouldHaveSize 3
        obs.topology.seats shouldHaveSize 3
        obs.currentSeatWind shouldBe obs.topology.firstSeatWind
        val dealAmount = players.first().closeHand.size
        val totalTiles = wallTiles.size
        obs.wall.size shouldBe totalTiles - (players.size * dealAmount)
        obs.discards shouldBe obs.topology.seats.associateWith { emptyList() }
        obs.lastAction shouldBe LastAction.None
        obs.availableActions shouldBe emptyList()
    }

    test("Match.start should draw tile to current player and update available actions") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        val initialWallSize = match.observation.wall.size

        val stepResult = match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val wall = match.observation.wall
        val dealAmount = wall.standardDealAmount

        eastPlayer.closeHand shouldHaveSize dealAmount + 1
        match.observation.wall.size shouldBe initialWallSize - 1
        match.observation.availableActions shouldContain DiscardAction
        val nextWind = match.observation.topology.getShimocha(match.observation.currentSeatWind).getOrThrow()
        stepResult.nextWind shouldBe nextWind
        stepResult.isOver shouldBe false
    }

    test("submitDiscard should succeed for valid discard and update match state") {
        val players = List(3) { DummyPlayer() }
        // Use double wall tiles to ensure enough tiles for automatic draw after discard
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val westPlayer = getPlayerBySeat(players, WEST)

        // Clear all hands and give each player completely different tile suits
        // This ensures no intercepts possible (Pon requires 3 same tiles, Chii requires sequence in same suit)
        eastPlayer.closeHand.clear()
        southPlayer.closeHand.clear()
        westPlayer.closeHand.clear()

        val dealAmount = match.observation.wall.standardDealAmount

        // East gets only Man tiles (no Pin tiles)
        // Give East Man tiles with values 1-13 (wrap around since Man only 1-9)
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            eastPlayer.closeHand.add(Tile(Man, value, false))
        }

        // South gets only Pin tiles (no Man tiles)
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            southPlayer.closeHand.add(Tile(Pin, value, false))
        }

        // West gets a mix but ensure no tile overlaps with East's discard
        // First, let East discard a specific tile we know is safe
        val safeDiscardTile = Tile(Man, 1, false)
        eastPlayer.closeHand[0] = safeDiscardTile // Replace first tile with our safe discard

        // Now give West tiles that cannot intercept Man1:
        // - No other Man1 tiles (for Pon)
        // - No Man2 or Man3 tiles (for Chii 1-2-3)
        // Give West only Pin and Sou tiles
        westPlayer.closeHand.clear()
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            // Alternate between Pin and Sou
            val tileType = if (i % 2 == 0) Pin else Sou
            westPlayer.closeHand.add(Tile(tileType, value, false))
        }

        val tile = safeDiscardTile

        val result = match.submitDiscard(eastPlayer, tile)
        result.shouldBeSuccess()

        val obs = match.observation
        obs.currentSeatWind shouldNotBe EAST
        obs.discards[EAST]?.shouldContain(tile)
        obs.lastAction shouldNotBe LastAction.None
        val wall = match.observation.wall
        eastPlayer.closeHand shouldHaveSize dealAmount - 1 // After discard
    }

    test("submitDiscard should fail for player not in match") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val outsider = DummyPlayer() // No seat assigned
        val tile = TestTileFactory.createMan(1)

        val result = match.submitDiscard(outsider, tile)
        result.shouldBeFailureWithPlayerNotInMatch()
    }

    test("submitDiscard should fail when not player's turn") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val southPlayer = getPlayerBySeat(players, SOUTH)
        val tile = TestTileFactory.createMan(1)

        val result = match.submitDiscard(southPlayer, tile)
        result.shouldBeFailureWithNotPlayersTurn()
    }

    test("submitDiscard should fail when tile not in player's hand") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val tileNotInHand = TestTileFactory.createMan(9) // Not in hand

        val result = match.submitDiscard(eastPlayer, tileNotInHand)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("DiscardAction.availableWhen should return true only for current player with tile in hand") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()

        val obs = match.observation
        val eastPlayer = getPlayerBySeat(players, EAST)
        val tile = eastPlayer.closeHand.first()

        DiscardAction.availableWhen(obs, eastPlayer, tile) shouldBe true
    }

    test("DiscardAction.availableWhen should return false for wrong player") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()

        val obs = match.observation
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val tile = TestTileFactory.createMan(1)

        DiscardAction.availableWhen(obs, southPlayer, tile) shouldBe false
    }

    test("DiscardAction.perform should move tile to discards and advance turn") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()

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

    test("DiscardAction.perform should fail when not player's turn") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()

        val obs = match.observation
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val tile = TestTileFactory.createMan(1)

        val result = DiscardAction.perform(obs, southPlayer, tile)
        result.shouldBeFailureWithNotPlayersTurn()
    }

    test("availableActions should only include DiscardAction when current player has tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()

        // Before start, no tiles
        match.observation.availableActions shouldBe emptyList()

        match.start().shouldBeSuccess()

        // After start, EAST has a tile
        match.observation.availableActions shouldContain DiscardAction

        val otherActions = match.observation.availableActions.filter { it != DiscardAction }
        val eastPlayer = getPlayerBySeat(players, EAST)
        val lastAction = match.observation.lastAction as LastAction.Draw
        val drawnTile = lastAction.tile

        // Valid post-draw actions for current player: TsuMo, Ankan, Kakan, RiichiAction
        otherActions.forEach { action ->
            when (action) {
                TsuMo -> {
                    TsuMo.availableWhen(match.observation, eastPlayer, drawnTile) shouldBe true
                }
                AnKan -> {
                    AnKan.availableWhen(match.observation, eastPlayer, drawnTile) shouldBe true
                }
                KaKan -> {
                    KaKan.availableWhen(match.observation, eastPlayer, drawnTile) shouldBe true
                }
                RiichiAction -> {
                    RiichiAction.availableWhen(match.observation, eastPlayer, drawnTile) shouldBe true
                }
                else -> {
                    throw AssertionError("Unexpected action after draw: $action")
                }
            }
        }
    }

    test("stub actions should not be available") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)

        // Replace east player's hand with a deterministic, non-winning hand
        // Give all Pin tiles to ensure no winning combinations with Man tile
        val dealAmount = match.observation.wall.standardDealAmount
        TestTileFactory.fillWithDeterministicNonWinningHand(eastPlayer, Pin, dealAmount)

        val obs = match.observation
        // Use a Man tile that is not in player's hand and cannot complete the hand
        val tile = TestTileFactory.createMan(1)

        Chii.availableWhen(obs, eastPlayer, tile) shouldBe false
        Pon.availableWhen(obs, eastPlayer, tile) shouldBe false
        Ron.availableWhen(obs, eastPlayer, tile) shouldBe false
        TsuMo.availableWhen(obs, eastPlayer, tile) shouldBe false
    }

    test("DiscardAction.perform should fail when tile not in hand") {
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

    test("DiscardAction.perform should fail when player has no seat") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val obs = match.observation
        val playerWithoutSeat = DummyPlayer() // seat = null
        val tile = TestTileFactory.createMan(1)

        val result = DiscardAction.perform(obs, playerWithoutSeat, tile)
        result.shouldBeFailureWithActionNotAvailable()
    }

    test("MatchObservation should be immutable snapshot") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()

        val obs1 = match.observation
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()
        val obs2 = match.observation

        obs1 shouldBe obs1
        obs2 shouldBe obs2
        obs1 shouldNotBe obs2
    }

    test("submitAction should work identically to submitDiscard") {
        val players = List(3) { DummyPlayer() }
        // Use double wall tiles to ensure enough tiles for automatic draw after discard
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val westPlayer = getPlayerBySeat(players, WEST)

        // Clear all hands and give each player completely different tile suits
        // This ensures no intercepts possible (Pon requires 3 same tiles, Chii requires sequence in same suit)
        eastPlayer.closeHand.clear()
        southPlayer.closeHand.clear()
        westPlayer.closeHand.clear()

        val dealAmount = match.observation.wall.standardDealAmount

        // East gets only Man tiles (no Pin tiles)
        // Give East Man tiles with values 1-13 (wrap around since Man only 1-9)
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            eastPlayer.closeHand.add(Tile(Man, value, false))
        }

        // South gets only Pin tiles (no Man tiles)
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            southPlayer.closeHand.add(Tile(Pin, value, false))
        }

        // West gets a mix but ensure no tile overlaps with East's discard
        // First, let East discard a specific tile we know is safe
        val safeDiscardTile = Tile(Man, 1, false)
        eastPlayer.closeHand[0] = safeDiscardTile // Replace first tile with our safe discard

        // Now give West tiles that cannot intercept Man1:
        // - No other Man1 tiles (for Pon)
        // - No Man2 or Man3 tiles (for Chii 1-2-3)
        // Give West only Pin tiles (Sou not in create40Wall)
        westPlayer.closeHand.clear()
        for (i in 1..dealAmount) {
            val value = ((i - 1) % 9) + 1
            westPlayer.closeHand.add(Tile(Pin, value, false))
        }

        val tile = safeDiscardTile

        val result = match.submitAction(eastPlayer, DiscardAction, tile)
        result.shouldBeSuccess()

        match.observation.currentSeatWind shouldNotBe EAST
        match.observation.discards[EAST] shouldNotBe null
        match.observation.discards[EAST]!! shouldContain tile
        match.observation.lastAction shouldNotBe LastAction.None
    }

    test("checkOver should return false (stub implementation)") {
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withPlayers(3).build()
        match.checkOver() shouldBe false
    }

    test("multiple players can be available for interrupt actions") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val westPlayer = getPlayerBySeat(players, WEST)

        // Give both south and west two Man1 tiles for pon
        southPlayer.closeHand.clear()
        westPlayer.closeHand.clear()
        val man1a = TestTileFactory.createMan(1)
        val man1b = TestTileFactory.createMan(1)
        val man1c = TestTileFactory.createMan(1)
        val man1d = TestTileFactory.createMan(1)
        southPlayer.closeHand.addAll(listOf(man1a, man1b))
        westPlayer.closeHand.addAll(listOf(man1c, man1d))

        // East discards Man1
        eastPlayer.closeHand.clear()
        val discardedMan1 = TestTileFactory.createMan(1)
        eastPlayer.closeHand.add(discardedMan1)
        match.submitDiscard(eastPlayer, discardedMan1).shouldBeSuccess()

        // Both south and west should have Pon available
        Pon.availableWhen(match.observation, southPlayer, discardedMan1) shouldBe true
        Pon.availableWhen(match.observation, westPlayer, discardedMan1) shouldBe true

        // South calls Pon first
        match.submitAction(southPlayer, Pon, discardedMan1).shouldBeSuccess()

        // Now Pon should no longer be available for west (discard removed)
        // Need to get updated observation
        val updatedObs = match.observation
        Pon.availableWhen(updatedObs, westPlayer, discardedMan1) shouldBe false
    }

    test("action priority: Ron takes precedence over Pon and Chii") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()

        val eastPlayer = getPlayerBySeat(players, EAST)
        val southPlayer = getPlayerBySeat(players, SOUTH)
        val westPlayer = getPlayerBySeat(players, WEST)

        // Setup: South has winning hand (needs one more Man2)
        southPlayer.closeHand.clear()
        // Give south 3 Man2, 3 Man3, 3 Pin4, 3 Pin5 (total 12 tiles, all non-yaochuhai for Tanyao yaku)
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(2)) }
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createMan(3)) }
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createPin(4)) }
        repeat(3) { southPlayer.closeHand.add(TestTileFactory.createPin(5)) }

        // West has two Man2 tiles for Pon
        westPlayer.closeHand.clear()
        westPlayer.closeHand.addAll(listOf(TestTileFactory.createMan(2), TestTileFactory.createMan(2)))

        // East discards Man2 (winning tile for south, also pon tile for west)
        eastPlayer.closeHand.clear()
        val winningTile = TestTileFactory.createMan(2)
        eastPlayer.closeHand.add(winningTile)
        match.submitDiscard(eastPlayer, winningTile).shouldBeSuccess()

        // Both Ron (south) and Pon (west) should be available
        Ron.availableWhen(match.observation, southPlayer, winningTile) shouldBe true
        Pon.availableWhen(match.observation, westPlayer, winningTile) shouldBe true

        // South calls Ron (should succeed even if west tries Pon later)
        match.submitAction(southPlayer, Ron, winningTile).shouldBeSuccess()

        // Game should be over after Ron
        // West cannot call Pon after Ron (game ended)
        val updatedObs = match.observation
        Pon.availableWhen(updatedObs, westPlayer, winningTile) shouldBe false
    }

    test("error wrapping should preserve MatchError type") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        (match.start() as Result<StepResult, ActionError>).shouldBeSuccess()

        val outsider = DummyPlayer()
        val tile = TestTileFactory.createMan(1)

        val result = match.submitDiscard(outsider, tile)
        result.shouldBeFailureWithPlayerNotInMatch()
    }

    test("Match.create should fail when player count does not match seat count") {
        // Create enough tiles for 4 players (4 * 13 = 52 tiles) to avoid WallError
        val baseWall = TestTileFactory.create40Wall() // 40 tiles
        val extraTiles = TestTileFactory.create40Wall().take(12) // additional 12 tiles
        val sufficientTiles = baseWall + extraTiles // 52 tiles

        // Create a rule set with 3 seats but enough tiles for 4 players
        val ruleSet = createSimpleRuleSet(sufficientTiles)
        // Try to create match with 4 players (seat count is 3)
        val players = List(4) { DummyPlayer() }
        val result = Match.create(ruleSet, emptyList(), players, shuffleWinds = false)
        result.shouldBeInstanceOf<Result.Failure<ConfigurationError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ConfigurationError.MatchConfigurationError.PlayerCountMismatch>()
        val mismatch = error as ConfigurationError.MatchConfigurationError.PlayerCountMismatch
        mismatch.playerCount shouldBe 4
        mismatch.seatCount shouldBe 3
    }

    test("isRoundOver returns true when wall is empty") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()
        
        match.isRoundOver(LastAction.Discard(TestTileFactory.createMan(1), players[0])) shouldBe true
    }

    test("endRound increments honba on exhaustive draw with no winner") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()
        
        val result = match.endRound(winnerSeat = null)
        result.shouldBeSuccess()
        
        match.observation.roundRotationStatus.honba shouldBe 1
        match.observation.honbaSticks shouldBe 1
    }

    test("startNextRound fails when wall has insufficient tiles") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()
        
        try {
            match.startNextRound()
            io.kotest.assertions.fail("Expected exception but none was thrown")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    test("isMatchOver returns true when currentRound >= totalRounds") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        
        match.isMatchOver shouldBe true
    }

    test("observation.availableActions handles null mask for current player") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
        match.start().shouldBeSuccess()
        
        val obs = match.observation
        obs.availableActions shouldContain DiscardAction
    }

    test("Match.create assigns seats randomly when shuffleWinds is true") {
        val players = List(3) { DummyPlayer() }
        val wallTiles = TestTileFactory.create40Wall()
        val ruleSet = createSimpleRuleSet(wallTiles)
        
        val result = Match.create(ruleSet, emptyList(), players, shuffleWinds = true)
        result.shouldBeInstanceOf<Result.Success<Match>>()
        
        val match = (result as Result.Success).value
        
        val seats = players.map { it.seat }.toSet()
        seats.size shouldBe 3
        seats shouldContain StandardWind.EAST
        seats shouldContain StandardWind.SOUTH
        seats shouldContain StandardWind.WEST
    }
})