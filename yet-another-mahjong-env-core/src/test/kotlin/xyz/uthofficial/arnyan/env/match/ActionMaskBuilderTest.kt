package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import xyz.uthofficial.arnyan.env.match.actions.AnKan
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.KaKan
import xyz.uthofficial.arnyan.env.match.actions.MinKan
import xyz.uthofficial.arnyan.env.match.actions.NukiPei
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo

import xyz.uthofficial.arnyan.env.scoring.StandardScoringCalculator
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.StandardTileWall
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.CyclicTableTopology
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.StandardWind.*
import xyz.uthofficial.arnyan.env.wind.Wind as SeatWind

class ActionMaskBuilderTest : FunSpec({

    fun createTestState(
        players: List<DummyPlayer>,
        currentSeat: StandardWind = EAST,
        lastAction: LastAction = LastAction.None
    ): MatchState {
        val topology = CyclicTableTopology(listOf(EAST, SOUTH, WEST))
        val wall = StandardTileWall(standardDealAmount = 13)
        wall.addAll(TestTileFactory.create40Wall() + TestTileFactory.create40Wall())
        
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
            lastAction = lastAction,
            yakuConfiguration = createSimpleYakuConfiguration(),
            scoringCalculator = StandardScoringCalculator(),
            furitenPlayers = mutableSetOf(),
            temporaryFuritenPlayers = mutableSetOf(),
            passedPlayers = mutableSetOf(),
            availableActionsMaskPerPlayer = mutableMapOf(),
            riichiSticks = 0,
            honbaSticks = 0,
            doraIndicators = mutableListOf()
        )
    }

    fun createActionMaskBuilder(): ActionMaskBuilder {
        val validator = ActionValidator()
        return ActionMaskBuilder(validator)
    }

    test("maskToActions returns empty list for mask 0") {
        val builder = createActionMaskBuilder()
        val actions = builder.maskToActions(0)
        actions shouldBe emptyList()
    }

    test("maskToActions extracts correct actions from mask") {
        val builder = createActionMaskBuilder()
        val mask = Action.ID_DISCARD or Action.ID_TSUMO
        val actions = builder.maskToActions(mask)
        actions shouldContain DiscardAction
        actions shouldContain TsuMo
    }

    test("updateAvailableActions skips players with null seat") {
        val players = listOf(DummyPlayer(seat = null))
        val topology = CyclicTableTopology(emptyList())
        val wall = StandardTileWall(standardDealAmount = 13)
        wall.addAll(TestTileFactory.create40Wall())
        
        val state = MatchState(
            players = players,
            wall = wall,
            topology = topology,
            currentSeatWind = EAST,
            roundRotationStatus = RoundRotationStatus(EAST, 1, 0),
            discards = mutableMapOf(),
            lastAction = LastAction.None,
            yakuConfiguration = createSimpleYakuConfiguration(),
            scoringCalculator = StandardScoringCalculator(),
            furitenPlayers = mutableSetOf(),
            temporaryFuritenPlayers = mutableSetOf(),
            passedPlayers = mutableSetOf(),
            availableActionsMaskPerPlayer = mutableMapOf(),
            riichiSticks = 0,
            honbaSticks = 0,
            doraIndicators = mutableListOf()
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        state.availableActionsMaskPerPlayer.size shouldBe 0
    }

    test("updateAvailableActions skips players in passedPlayers") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        state.passedPlayers.add(EAST)
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        state.availableActionsMaskPerPlayer[EAST] shouldBe 0
    }

    test("updateAvailableActions detects Chii interrupt") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(3)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val southMask = state.availableActionsMaskPerPlayer[SOUTH] ?: 0
        southMask shouldBe 0
    }

    test("updateAvailableActions detects Pon interrupt") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(4), TestTileFactory.createMan(4),
            TestTileFactory.createPin(5), TestTileFactory.createPin(5),
            TestTileFactory.createPin(6), TestTileFactory.createPin(6),
            TestTileFactory.createSou(7), TestTileFactory.createSou(7)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val southMask = state.availableActionsMaskPerPlayer[SOUTH] ?: 0
        (southMask and Action.ID_PON) shouldNotBe 0
    }

    test("updateAvailableActions detects Ron interrupt") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createPin(3), TestTileFactory.createPin(3), TestTileFactory.createPin(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Discard(TestTileFactory.createPin(4), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val southMask = state.availableActionsMaskPerPlayer[SOUTH] ?: 0
        (southMask and (Action.ID_RON or Action.ID_PASS)) shouldNotBe 0
    }

    test("updateAvailableActions restricts riichi player mask to Tsumo/Ron/Pass") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].isRiichiDeclared = true
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(
            players,
            lastAction = LastAction.Draw(TestTileFactory.createMan(1), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        eastMask shouldBe (eastMask and (Action.ID_TSUMO or Action.ID_RON or Action.ID_PASS or Action.ID_DISCARD))
    }

    test("updateAvailableActions adds DiscardAction for current player") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(players)
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_DISCARD) shouldNotBe 0
    }

    test("updateAvailableActions adds DiscardAction for current player") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(players)
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_DISCARD) shouldNotBe 0
    }

    test("updateAvailableActions adds TsuMo when drawn tile can win") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createPin(3), TestTileFactory.createPin(3), TestTileFactory.createPin(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Draw(TestTileFactory.createPin(4), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_TSUMO) shouldNotBe 0
    }

    test("updateAvailableActions adds KaKan when available") {
        val players = listOf(DummyPlayer(seat = EAST))
        val kanGroup = listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        )
        players[0].openHand.add(kanGroup)
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(
            players,
            lastAction = LastAction.Draw(TestTileFactory.createMan(1), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_KAKAN) shouldBe 0
    }

    test("updateAvailableActions adds RiichiAction when available") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createMan(3), TestTileFactory.createMan(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4),
            TestTileFactory.createPin(5), TestTileFactory.createPin(5),
            TestTileFactory.createPin(6), TestTileFactory.createPin(6),
            TestTileFactory.createSou(7)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Draw(TestTileFactory.createSou(7), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_RIICHI) shouldBe 0
    }

    test("updateAvailableActions adds NukiPei when available") {
        val players = listOf(DummyPlayer(seat = EAST))
        val northTile = Tile(xyz.uthofficial.arnyan.env.tile.Wind, 4, false)
        players[0].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createMan(3), TestTileFactory.createMan(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4),
            TestTileFactory.createPin(5), TestTileFactory.createPin(5),
            TestTileFactory.createPin(6), TestTileFactory.createPin(6),
            northTile
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Draw(northTile, players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_NUKI) shouldNotBe 0
    }

    test("updateAvailableActions adds Pass action for interrupt phase") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(4), TestTileFactory.createMan(4),
            TestTileFactory.createPin(5), TestTileFactory.createPin(5),
            TestTileFactory.createPin(6), TestTileFactory.createPin(6),
            TestTileFactory.createSou(7), TestTileFactory.createSou(7)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Discard(TestTileFactory.createMan(1), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val southMask = state.availableActionsMaskPerPlayer[SOUTH] ?: 0
        (southMask and Action.ID_PASS) shouldNotBe 0
    }

    test("updateAvailableActions applies interrupt priority for Ron") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players[1].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createPin(3), TestTileFactory.createPin(3), TestTileFactory.createPin(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4)
        ))
        players[2].closeHand.addAll(listOf(
            TestTileFactory.createMan(1), TestTileFactory.createMan(1), TestTileFactory.createMan(1),
            TestTileFactory.createMan(2), TestTileFactory.createMan(2), TestTileFactory.createMan(2),
            TestTileFactory.createPin(3), TestTileFactory.createPin(3), TestTileFactory.createPin(3),
            TestTileFactory.createPin(4), TestTileFactory.createPin(4)
        ))
        val state = createTestState(
            players,
            lastAction = LastAction.Discard(TestTileFactory.createPin(4), players[0])
        )
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val southMask = state.availableActionsMaskPerPlayer[SOUTH] ?: 0
        val westMask = state.availableActionsMaskPerPlayer[WEST] ?: 0
        (southMask and (Action.ID_RON or Action.ID_PASS)) shouldNotBe 0
        westMask shouldBe 0
    }

    test("updateAvailableActions handles no last action") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].closeHand.add(TestTileFactory.createMan(1))
        val state = createTestState(players, lastAction = LastAction.None)
        val builder = createActionMaskBuilder()
        
        builder.updateAvailableActions(state)
        val eastMask = state.availableActionsMaskPerPlayer[EAST] ?: 0
        (eastMask and Action.ID_DISCARD) shouldNotBe 0
    }
})
