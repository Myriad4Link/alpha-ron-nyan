package xyz.uthofficial.arnyan.simplified

import ai.djl.ndarray.NDManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.CyclicTableTopology
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import xyz.uthofficial.arnyan.env.yaku.StandardYakuRule
import java.util.UUID

class SanmaActionEncoderTest : FunSpec({
    
    val encoder = SanmaActionEncoder()
    
    test("encode discard action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createDiscardScenario()
            
            val fivePin = createPin(5)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.DiscardAction, fivePin, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            for (i in 0 until 27) {
                if (i == 9) {
                    encoded.getFloat(i.toLong()) shouldBe 1.0f
                } else {
                    encoded.getFloat(i.toLong()) shouldBe 0.0f
                }
            }
            
            for (i in 27 until 38) {
                if (i == 31) {
                    encoded.getFloat(i.toLong()) shouldBe 1.0f
                } else {
                    encoded.getFloat(i.toLong()) shouldBe 0.0f
                }
            }
        }
    }
    
    test("encode riichi action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createRiichiScenario()
            
            val southWind = createWind(2)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.RiichiAction, southWind, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(24.toLong()) shouldBe 1.0f
            
            encoded.getFloat(33.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode pon action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createPonScenario()
            
            val threeSou = createSou(3)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.Pon, threeSou, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(16.toLong()) shouldBe 1.0f
            
            encoded.getFloat(28.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode chii action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createChiiScenario()
            
            val fourPin = createPin(4)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.Chii, fourPin, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(8.toLong()) shouldBe 1.0f
            
            encoded.getFloat(27.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode ron action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createRonScenario()
            
            val sevenSou = createSou(7)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.Ron, sevenSou, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(20.toLong()) shouldBe 1.0f
            
            encoded.getFloat(29.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode tsumo action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createTsumoScenario()
            
            val ninePin = createPin(9)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.TsuMo, ninePin, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(13.toLong()) shouldBe 1.0f
            
            encoded.getFloat(30.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode pass action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createPassScenario()
            
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.PassAction, null, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            for (i in 0 until 27) {
                encoded.getFloat(i.toLong()) shouldBe 0.0f
            }
            
            encoded.getFloat(32.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode ankan action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createAnkanScenario()
            
            val eastWind = createWind(1)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.AnKan, eastWind, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(23.toLong()) shouldBe 1.0f
            
            encoded.getFloat(34.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode minkan action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createMinkanScenario()
            
            val whiteDragon = createDragon(1)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.MinKan, whiteDragon, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(0.toLong()) shouldBe 1.0f
            
            encoded.getFloat(35.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode kakan action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createKakanScenario()
            
            val redDragon = createDragon(3)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.KaKan, redDragon, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(2.toLong()) shouldBe 1.0f
            
            encoded.getFloat(36.toLong()) shouldBe 1.0f
        }
    }
    
    test("encode nuki action correctly") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createNukiScenario()
            
            val northWind = createWind(4)
            val encoded = encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.NukiPei, northWind, observation, player)
            
            encoded.shape[0] shouldBe 38L
            
            encoded.getFloat(26.toLong()) shouldBe 1.0f
            
            encoded.getFloat(37.toLong()) shouldBe 1.0f
        }
    }
    
    test("throw ActionNotAvailable for invalid action") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createDiscardScenario()
            
            shouldThrow<ActionEncodingError.ActionNotAvailable> {
                encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.Pon, createPin(5), observation, player)
            }
        }
    }
    
    test("throw SubjectTileNotInHand for discard tile not in hand") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createDiscardScenario()
            
            val tileNotInHand = createSou(1)
            shouldThrow<ActionEncodingError.SubjectTileNotInHand> {
                encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.DiscardAction, tileNotInHand, observation, player)
            }
        }
    }
    
    test("throw SubjectTileMismatch for pon with wrong tile") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createPonScenario()
            
            val wrongTile = createPin(5)
            shouldThrow<ActionEncodingError.SubjectTileMismatch> {
                encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.Pon, wrongTile, observation, player)
            }
        }
    }
    
    test("throw PassWithSubject for pass with non-null subject") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createPassScenario()
            
            shouldThrow<ActionEncodingError.PassWithSubject> {
                encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.PassAction, createPin(1), observation, player)
            }
        }
    }
    
    test("throw MissingSubjectTile for discard with null subject") {
        val manager = NDManager.newBaseManager()
        manager.use {
            val (observation, player) = createDiscardScenario()
            
            shouldThrow<ActionEncodingError.MissingSubjectTile> {
                encoder.encode(manager, xyz.uthofficial.arnyan.env.match.actions.DiscardAction, null, observation, player)
            }
        }
    }
    
    test("validate sanma tile mapping excludes 2m-8m") {
        val manager = NDManager.newBaseManager()
        manager.use {
            for (manValue in 2..8) {
                val manTile = createMan(manValue)
                val registryIndex = manTile.value + 2
                val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
                sanmaIndex shouldBe null
            }
        }
    }
})

private fun createDiscardScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createPin(1),
            createPin(3),
            createPin(5),
            createSou(7),
            createSou(9),
            createWind(1),
            createDragon(1)
        ),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createPin(2), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.DiscardAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 5
    )
    
    return observation to player
}

private fun createRiichiScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createMan(1),
            createMan(2),
            createMan(3),
            createPin(1),
            createPin(2),
            createPin(3),
            createSou(1),
            createSou(2),
            createSou(3),
            createDragon(1),
            createDragon(1),
            createDragon(1),
            createWind(2),
            createWind(2)
        ),
        seat = StandardWind.EAST,
        score = 25000,
        isRiichiDeclared = false,
        openHand = mutableListOf()
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createWind(2), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.DiscardAction, xyz.uthofficial.arnyan.env.match.actions.RiichiAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 10
    )
    
    return observation to player
}

private fun createPonScenario(): Pair<MatchObservation, TestPlayer> {
    val southPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createSou(3),
            createSou(3),
            createPin(5)
        ),
        seat = StandardWind.SOUTH,
        score = 25000
    )
    
    val eastPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createPin(1),
            createPin(2)
        ),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(eastPlayer, southPlayer),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Discard(createSou(3), eastPlayer),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.Pon, xyz.uthofficial.arnyan.env.match.actions.PassAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 3
    )
    
    return observation to southPlayer
}

private fun createChiiScenario(): Pair<MatchObservation, TestPlayer> {
    val eastPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createPin(3),
            createPin(5)
        ),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val southPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createSou(7)
        ),
        seat = StandardWind.SOUTH,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(eastPlayer, southPlayer),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.SOUTH,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Discard(createPin(4), southPlayer),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.Chii, xyz.uthofficial.arnyan.env.match.actions.PassAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 4
    )
    
    return observation to eastPlayer
}

private fun createRonScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createMan(1),
            createMan(1),
            createMan(2),
            createMan(3),
            createPin(5),
            createPin(6),
            createPin(7),
            createSou(8),
            createSou(9),
            createWind(1),
            createWind(1),
            createDragon(1),
            createDragon(1)
        ),
        seat = StandardWind.SOUTH,
        score = 25000
    )
    
    val eastPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(eastPlayer, player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Discard(createSou(7), eastPlayer),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.Ron, xyz.uthofficial.arnyan.env.match.actions.PassAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 15
    )
    
    return observation to player
}

private fun createTsumoScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createMan(1),
            createMan(1),
            createMan(2),
            createMan(3),
            createPin(5),
            createPin(6),
            createPin(7),
            createSou(8),
            createSou(8),
            createWind(1),
            createWind(1),
            createDragon(1),
            createDragon(1)
        ),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createPin(9), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.TsuMo, xyz.uthofficial.arnyan.env.match.actions.DiscardAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 12
    )
    
    return observation to player
}

private fun createPassScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createPin(1)
        ),
        seat = StandardWind.WEST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Discard(createSou(5), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.PassAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 8
    )
    
    return observation to player
}

private fun createAnkanScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createWind(1),
            createWind(1),
            createWind(1),
            createWind(1),
            createPin(5)
        ),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createWind(1), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.AnKan, xyz.uthofficial.arnyan.env.match.actions.DiscardAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 6
    )
    
    return observation to player
}

private fun createMinkanScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createDragon(1),
            createDragon(1),
            createDragon(1)
        ),
        seat = StandardWind.SOUTH,
        score = 25000,
        openHand = mutableListOf(listOf(
            createDragon(1),
            createDragon(1),
            createDragon(1)
        ))
    )
    
    val eastPlayer = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(),
        seat = StandardWind.EAST,
        score = 25000
    )
    
    val observation = MatchObservation(
        players = listOf(eastPlayer, player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Discard(createDragon(1), eastPlayer),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.MinKan, xyz.uthofficial.arnyan.env.match.actions.PassAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 9
    )
    
    return observation to player
}

private fun createKakanScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createDragon(3)
        ),
        seat = StandardWind.EAST,
        score = 25000,
        openHand = mutableListOf(listOf(
            createDragon(3),
            createDragon(3),
            createDragon(3)
        ))
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createDragon(3), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.KaKan, xyz.uthofficial.arnyan.env.match.actions.DiscardAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 11
    )
    
    return observation to player
}

private fun createNukiScenario(): Pair<MatchObservation, TestPlayer> {
    val player = TestPlayer(
        id = UUID.randomUUID(),
        closeHand = mutableListOf(
            createWind(4),
            createPin(5)
        ),
        seat = StandardWind.EAST,
        score = 25000,
        nukiCount = 0
    )
    
    val observation = MatchObservation(
        players = listOf(player),
        wall = TestTileWall(emptyList()),
        topology = CyclicTableTopology(listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)),
        currentSeatWind = StandardWind.EAST,
        roundRotationStatus = RoundRotationStatus(StandardWind.EAST, 1, 0),
        discards = emptyMap(),
        lastAction = LastAction.Draw(createWind(4), player),
        availableActions = listOf(xyz.uthofficial.arnyan.env.match.actions.NukiPei, xyz.uthofficial.arnyan.env.match.actions.DiscardAction),
        yakuConfiguration = xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build(),
        turnCount = 7
    )
    
    return observation to player
}

private fun createMan(value: Int): Tile = Tile(xyz.uthofficial.arnyan.env.tile.Man, value)
private fun createPin(value: Int): Tile = Tile(xyz.uthofficial.arnyan.env.tile.Pin, value)
private fun createSou(value: Int): Tile = Tile(xyz.uthofficial.arnyan.env.tile.Sou, value)
private fun createWind(value: Int): Tile = Tile(xyz.uthofficial.arnyan.env.tile.Wind, value)
private fun createDragon(value: Int): Tile = Tile(xyz.uthofficial.arnyan.env.tile.Dragon, value)

data class TestPlayer(
    override val id: UUID,
    override val closeHand: MutableList<Tile>,
    override val openHand: MutableList<List<Tile>> = mutableListOf(),
    override val currentMentsusComposition: MutableList<List<Mentsu>> = mutableListOf(),
    override var seat: xyz.uthofficial.arnyan.env.wind.Wind?,
    override var score: Int,
    override var isRiichiDeclared: Boolean = false,
    override var riichiSticksDeposited: Int = 0,
    override var nukiCount: Int = 0
) : Player

data class TestTileWall(
    private val tiles: List<Tile>
) : xyz.uthofficial.arnyan.env.tile.ReadOnlyTileWall {
    override val size: Int = tiles.size
    override val standardDealAmount: Int = 13
    override val tileWall: List<Tile> = tiles
    override val doraIndicators: List<Tile> = emptyList()
    override val deadWallRemaining: Int = 0
}
