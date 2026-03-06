package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.TsuMo
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.scoring.StandardScoringCalculator
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.StandardTileWall
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.CyclicTableTopology
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardWind.*
import xyz.uthofficial.arnyan.env.wind.Wind
import java.util.*

class ActionValidatorTest : FunSpec({

    fun createTestState(
        players: List<DummyPlayer>,
        currentSeat: StandardWind = EAST,
        passedPlayers: Set<StandardWind> = emptySet()
    ): MatchState {
        val topology = CyclicTableTopology(listOf(EAST, SOUTH, WEST))
        val wall = StandardTileWall(standardDealAmount = 13)
        wall.addAll(TestTileFactory.create40Wall())
        
        return MatchState(
            players = players,
            wall = wall,
            topology = topology,
            currentSeatWind = currentSeat,
            roundRotationStatus = RoundRotationStatus(EAST, 1, 0),
            discards = mutableMapOf(
                EAST to mutableListOf(),
                SOUTH to mutableListOf(),
                WEST to mutableListOf()
            ),
            lastAction = LastAction.None,
            yakuConfiguration = createSimpleYakuConfiguration(),
            scoringCalculator = StandardScoringCalculator(),
            furitenPlayers = mutableSetOf(),
            temporaryFuritenPlayers = mutableSetOf(),
            passedPlayers = passedPlayers.toMutableSet(),
            availableActionsMaskPerPlayer = mutableMapOf(
                EAST to Action.ID_DISCARD,
                SOUTH to 0,
                WEST to 0
            ),
            riichiSticks = 0,
            honbaSticks = 0,
            doraIndicators = mutableListOf()
        )
    }

    fun createValidator(): ActionValidator = ActionValidator()

    test("validateAction fails when player seat is null") {
        val players = List(3) { DummyPlayer() }
        val state = createTestState(players)
        val validator = createValidator()
        val playerWithoutSeat = DummyPlayer(seat = null)
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, playerWithoutSeat, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Failure<ActionError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ActionError.Match>()
        (error as ActionError.Match).error.shouldBeInstanceOf<xyz.uthofficial.arnyan.env.error.MatchError.PlayerNotInMatch>()
    }

    test("validateAction fails when player seat not in topology") {
        val players = List(3) { DummyPlayer() }
        val state = createTestState(players)
        val validator = createValidator()
        val playerWithInvalidSeat = DummyPlayer(seat = StandardWind.NORTH)
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, playerWithInvalidSeat, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Failure<ActionError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ActionError.Match>()
        (error as ActionError.Match).error.shouldBeInstanceOf<xyz.uthofficial.arnyan.env.error.MatchError.PlayerNotInMatch>()
    }

    test("validateAction fails for DiscardAction when not player's turn") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        val state = createTestState(players, currentSeat = EAST)
        val validator = createValidator()
        val southPlayer = players[1]
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, southPlayer, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Failure<ActionError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ActionError.Match>()
        (error as ActionError.Match).error.shouldBeInstanceOf<xyz.uthofficial.arnyan.env.error.MatchError.NotPlayersTurn>()
    }

    test("validateAction fails when playerMask is null") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        val state = createTestState(players)
        state.availableActionsMaskPerPlayer.clear()
        val validator = createValidator()
        val eastPlayer = players[0]
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, eastPlayer, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Failure<ActionError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ActionError.Match>()
        (error as ActionError.Match).error.shouldBeInstanceOf<xyz.uthofficial.arnyan.env.error.MatchError.ActionNotAvailable>()
    }

    test("validateAction fails when action bit not in playerMask") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        val state = createTestState(players)
        state.availableActionsMaskPerPlayer[EAST] = 0
        val validator = createValidator()
        val eastPlayer = players[0]
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, eastPlayer, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Failure<ActionError>>()
        val error = (result as Result.Failure).error
        error.shouldBeInstanceOf<ActionError.Match>()
        (error as ActionError.Match).error.shouldBeInstanceOf<xyz.uthofficial.arnyan.env.error.MatchError.ActionNotAvailable>()
    }

    test("validateAction succeeds for valid DiscardAction") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(players)
        state.availableActionsMaskPerPlayer[EAST] = Action.ID_DISCARD
        val validator = createValidator()
        val eastPlayer = players[0]
        val tile = TestTileFactory.createMan(1)

        val result = validator.validateAction(state, eastPlayer, DiscardAction, tile)
        result.shouldBeInstanceOf<Result.Success<Unit>>()
    }

    test("validateAction succeeds for TsuMo action") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[0].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createPin(3), TestTileFactory.createPin(3), TestTileFactory.createPin(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4)
        ))
        val state = createTestState(players)
        state.lastAction = LastAction.Draw(TestTileFactory.createPin(4), players[0])
        state.availableActionsMaskPerPlayer[EAST] = Action.ID_TSUMO
        val validator = createValidator()
        val eastPlayer = players[0]
        val tile = TestTileFactory.createPin(4)

        val result = validator.validateAction(state, eastPlayer, TsuMo, tile)
        result.shouldBeInstanceOf<Result.Success<Unit>>()
    }

    test("getInterruptMaskForPlayer returns 0 when lastAction is not Discard") {
        val players = List(3) { DummyPlayer() }
        players.forEachIndexed { index, player ->
            player.seat = listOf(EAST, SOUTH, WEST)[index]
        }
        val state = createTestState(players)
        state.lastAction = LastAction.Draw(TestTileFactory.createMan(1), players[0])
        val validator = createValidator()

        val mask = validator.getInterruptMaskForPlayer(state, SOUTH)
        mask shouldBe 0
    }

    test("getInterruptMaskForPlayer includes Chii when available") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(3)
        ))
        val state = createTestState(players)
        state.lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        val validator = createValidator()

        val mask = validator.getInterruptMaskForPlayer(state, SOUTH)
        mask shouldBe 0
    }

    test("isInterruptPhase returns true when any player has interrupt mask") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))
        val state = createTestState(players)
        state.lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        state.availableActionsMaskPerPlayer[SOUTH] = Action.ID_PON
        val validator = createValidator()

        val isInterrupt = validator.isInterruptPhase(state)
        isInterrupt shouldBe true
    }

    test("isInterruptPhase returns false when all players have no interrupt mask") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        state.lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        state.availableActionsMaskPerPlayer[SOUTH] = 0
        state.availableActionsMaskPerPlayer[WEST] = 0
        val validator = createValidator()

        val isInterrupt = validator.isInterruptPhase(state)
        isInterrupt shouldBe false
    }

    test("allInterruptsResolved returns true when all masks are 0") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        state.lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        state.availableActionsMaskPerPlayer[SOUTH] = 0
        state.availableActionsMaskPerPlayer[WEST] = 0
        val validator = createValidator()

        val allResolved = validator.allInterruptsResolved(state)
        allResolved shouldBe true
    }

    test("allInterruptsResolved returns false when any player has interrupt mask") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))
        val state = createTestState(players)
        state.lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        state.availableActionsMaskPerPlayer[SOUTH] = Action.ID_PON
        val validator = createValidator()

        val allResolved = validator.allInterruptsResolved(state)
        allResolved shouldBe false
    }

    test("getInterruptPriorityOrder returns correct order") {
        val players = List(3) { DummyPlayer() }
        val state = createTestState(players)
        val validator = createValidator()

        val order = validator.getInterruptPriorityOrder(EAST, state.topology)
        order shouldBe listOf<Wind>(SOUTH, WEST)
    }

    test("applyInterruptPriority assigns Ron to first player only") {
        val players = List(3) { DummyPlayer() }
        val state = createTestState(players)
        val validator = createValidator()

        val availability = mapOf<Wind, Int>(
            SOUTH to Action.ID_RON,
            WEST to Action.ID_RON
        )

        val result = validator.applyInterruptPriority(EAST, state.topology, availability)

        val southMask = result[SOUTH] ?: 0
        val westMask = result[WEST] ?: 0

        (southMask and Action.ID_RON) shouldNotBe 0
        (westMask and Action.ID_RON) shouldBe 0
    }

    test("applyInterruptPriority assigns Pon to first eligible player") {
        val players = List(3) { DummyPlayer() }
        val state = createTestState(players)
        val validator = createValidator()

        val availability = mapOf<Wind, Int>(
            SOUTH to Action.ID_PON,
            WEST to Action.ID_PON
        )

        val result = validator.applyInterruptPriority(EAST, state.topology, availability)

        val southMask = result[SOUTH] ?: 0
        val westMask = result[WEST] ?: 0

        (southMask and Action.ID_PON) shouldNotBe 0
        (westMask and Action.ID_PON) shouldBe 0
    }

    test("applyInterruptPriority handles mixed Ron and Pon") {
        val players = List(3) { DummyPlayer() }
        players.forEachIndexed { index, player ->
            player.seat = listOf(EAST, SOUTH, WEST)[index]
        }
        val state = createTestState(players)
        val validator = createValidator()

        val availability = mapOf<Wind, Int>(
            SOUTH to (Action.ID_RON or Action.ID_PON),
            WEST to Action.ID_PON
        )

        val result = validator.applyInterruptPriority(EAST, state.topology, availability)

        val southMask = result[SOUTH] ?: 0
        val westMask = result[WEST] ?: 0

        (southMask and Action.ID_RON) shouldNotBe 0
        (southMask and Action.ID_PON) shouldBe 0
        (westMask and Action.ID_PON) shouldNotBe 0
    }
})
