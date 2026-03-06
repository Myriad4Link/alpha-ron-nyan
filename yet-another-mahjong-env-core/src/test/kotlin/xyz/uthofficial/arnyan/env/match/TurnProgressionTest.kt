package xyz.uthofficial.arnyan.env.match

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.match.actions.AnKan
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.KaKan
import xyz.uthofficial.arnyan.env.match.actions.MinKan
import xyz.uthofficial.arnyan.env.match.actions.NukiPei
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.result.Result
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
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import java.util.*

class TurnProgressionTest : FunSpec({

    fun createTestState(
        players: List<DummyPlayer>,
        currentSeat: StandardWind = EAST,
        wallTiles: List<Tile>? = null
    ): MatchState {
        val topology = CyclicTableTopology(listOf(EAST, SOUTH, WEST))
        val wall = StandardTileWall(standardDealAmount = 13)
        if (wallTiles != null) {
            wall.addAll(wallTiles)
        } else {
            wall.addAll(TestTileFactory.create40Wall() + TestTileFactory.create40Wall())
        }
        
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
            passedPlayers = mutableSetOf(),
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

    fun createTurnProgression(): TurnProgression {
        val validator = ActionValidator()
        val actionMaskBuilder = ActionMaskBuilder(validator)
        return TurnProgression(actionMaskBuilder, validator)
    }

    test("start draws tile and updates action mask") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val result = progression.start(state)
        result.shouldBeInstanceOf<Result.Success<StepResult>>()
        
        state.availableActionsMaskPerPlayer[EAST] shouldNotBe 0
    }

    test("start draws tile and updates state") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        val initialWallSize = state.wall.size
        
        val result = progression.start(state)
        result.shouldBeInstanceOf<Result.Success<StepResult>>()
        
        players[0].closeHand.size shouldBe 1
        state.wall.size shouldBe initialWallSize - 1
        state.lastAction.shouldBeInstanceOf<LastAction.Draw>()
    }

    test("computeExhaustiveDrawStateChanges uses first player as dealer when no EAST") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = SOUTH
        players[1].seat = WEST
        players[2].seat = NORTH
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val changes = progression.computeExhaustiveDrawStateChanges(state)
        
        changes.any { it is StateChange.UpdateHonbaSticks } shouldBe true
    }

    test("computeExhaustiveDrawStateChanges skips players with delta 0") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        state.riichiSticks = 0
        state.honbaSticks = 0
        val progression = createTurnProgression()
        
        val changes = progression.computeExhaustiveDrawStateChanges(state)
        
        changes.filterIsInstance<StateChange.UpdatePlayerScore>().forEach { change ->
            change.delta shouldNotBe 0
        }
    }

    test("checkOver returns true when wall is empty") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players, wallTiles = emptyList())
        val progression = createTurnProgression()
        
        val isOver = progression.checkOver(state)
        isOver shouldBe true
    }

    test("checkOver returns true when total kans >= playerCount") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        
        players[0].openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))
        players[1].openHand.add(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2)
        ))
        players[2].openHand.add(listOf(
            TestTileFactory.createMan(3),
            TestTileFactory.createMan(3),
            TestTileFactory.createMan(3),
            TestTileFactory.createMan(3)
        ))
        
        val progression = createTurnProgression()
        val isOver = progression.checkOver(state)
        isOver shouldBe true
    }

    test("checkOver returns true when all players declared riichi") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        players.forEach { it.isRiichiDeclared = true }
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val isOver = progression.checkOver(state)
        isOver shouldBe true
    }

    test("checkOver returns false when conditions not met") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val isOver = progression.checkOver(state)
        isOver shouldBe false
    }

    test("countTotalKans counts only 4-tile groups") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        
        players[0].openHand.add(listOf(
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1),
            TestTileFactory.createMan(1)
        ))
        players[0].openHand.add(listOf(
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2),
            TestTileFactory.createMan(2)
        ))
        
        val progression = createTurnProgression()
        val isOver = progression.checkOver(state)
        isOver shouldBe false
    }

    test("applyActionStateChanges handles RemoveOpenGroup with invalid index") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val change = StateChange.RemoveOpenGroup(EAST, 5)
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
    }

    test("applyActionStateChanges handles null playerDiscards") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        state.discards.remove(EAST)
        val progression = createTurnProgression()
        
        val change = StateChange.RemoveTileFromDiscards(EAST, TestTileFactory.createMan(1))
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
    }

    test("applyActionStateChanges handles missing tile in discards") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val change = StateChange.RemoveTileFromDiscards(EAST, TestTileFactory.createMan(9))
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
    }

    test("applyActionStateChanges applies UpdatePlayerScore") {
        val players = listOf(DummyPlayer(seat = EAST))
        players[0].score = 25000
        val state = createTestState(players)
        val progression = createTurnProgression()
        
        val change = StateChange.UpdatePlayerScore(EAST, 1000)
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
        players[0].score shouldBe 26000
    }

    test("applyActionStateChanges applies UpdateRiichiSticks") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        state.riichiSticks = 2
        val progression = createTurnProgression()
        
        val change = StateChange.UpdateRiichiSticks(1)
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
        state.riichiSticks shouldBe 3
    }

    test("applyActionStateChanges applies UpdateHonbaSticks") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        state.honbaSticks = 2
        val progression = createTurnProgression()
        
        val change = StateChange.UpdateHonbaSticks(1)
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
        state.honbaSticks shouldBe 3
    }

    test("applyActionStateChanges draws replacement tile on success") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        val initialHandSize = players[0].closeHand.size
        
        val change = StateChange.DrawReplacementTile(EAST)
        val stepResult = StepResult(state.toObservation(), EAST, false, listOf(change))
        
        progression.applyActionStateChanges(state, DiscardAction, players[0], stepResult)
        players[0].closeHand.size shouldBe initialHandSize + 1
    }

    test("PassAction adds player to passedPlayers") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, PassAction, players[0], stepResult)
        state.passedPlayers shouldContain EAST
    }

    test("RiichiAction sets isRiichiDeclared and riichiSticksDeposited") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, RiichiAction, players[0], stepResult)
        players[0].isRiichiDeclared shouldBe true
        players[0].riichiSticksDeposited shouldBe 1
    }

    test("Chii and Pon actions clear passedPlayers") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players)
        state.passedPlayers.add(SOUTH)
        state.passedPlayers.add(WEST)
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, Chii, players[0], stepResult)
        state.passedPlayers shouldBe emptySet()
        
        state.passedPlayers.add(SOUTH)
        progression.applyActionStateChanges(state, Pon, players[0], stepResult)
        state.passedPlayers shouldBe emptySet()
    }

    test("Kan actions reveal dora indicator on success") {
        val players = listOf(DummyPlayer(seat = EAST))
        val wallTiles = List(100) { TestTileFactory.createMan((it % 9) + 1) }
        val state = createTestState(players, wallTiles = wallTiles)
        state.wall.initializeDeadWall(14)
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, AnKan, players[0], stepResult)
        state.doraIndicators.size shouldBe 1
    }

    test("Kan actions handle dora reveal failure gracefully") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players, wallTiles = emptyList())
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, AnKan, players[0], stepResult)
        state.doraIndicators.size shouldBe 0
    }

    test("NukiPei removes player from temporaryFuritenPlayers") {
        val players = listOf(DummyPlayer(seat = EAST))
        val state = createTestState(players)
        state.temporaryFuritenPlayers.add(EAST)
        val progression = createTurnProgression()
        val stepResult = StepResult(state.toObservation(), EAST, false)
        
        progression.applyActionStateChanges(state, NukiPei, players[0], stepResult)
        state.temporaryFuritenPlayers shouldBe emptySet()
    }

    test("handlePostDiscardTurnAdvancement handles exhaustive draw") {
        val players = List(3) { DummyPlayer() }
        players[0].seat = EAST
        players[1].seat = SOUTH
        players[2].seat = WEST
        val state = createTestState(players, wallTiles = emptyList())
        val progression = createTurnProgression()
        
        val result = progression.handlePostDiscardTurnAdvancement(state)
        result.isOver shouldBe true
    }

    test("handlePostDiscardTurnAdvancement updates action mask") {
        val players = listOf(DummyPlayer(seat = EAST))
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val state = createTestState(players, wallTiles = wallTiles)
        val progression = createTurnProgression()
        
        val result = progression.handlePostDiscardTurnAdvancement(state)
        result.isOver shouldBe false
        state.availableActionsMaskPerPlayer[EAST] shouldNotBe 0
    }

    test("handlePostDiscardTurnAdvancement continues normal play") {
        val players = listOf(DummyPlayer(seat = EAST))
        val wallTiles = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()
        val state = createTestState(players, wallTiles = wallTiles)
        val progression = createTurnProgression()
        
        val result = progression.handlePostDiscardTurnAdvancement(state)
        result.isOver shouldBe false
        players[0].closeHand.size shouldBe 1
    }
})
