package xyz.uthofficial.arnyan.simplified

import ai.djl.ndarray.NDManager
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.types.Shape
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import xyz.uthofficial.arnyan.env.match.Match
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import java.util.UUID

data class TestPlayer(
    override val id: UUID = UUID.randomUUID(),
    override val closeHand: MutableList<Tile> = mutableListOf(),
    override val openHand: MutableList<List<Tile>> = mutableListOf(),
    override val currentMentsusComposition: MutableList<List<Mentsu>> = mutableListOf(),
    override var seat: Wind? = null,
    override var score: Int = 0,
    override var isRiichiDeclared: Boolean = false,
    override var riichiSticksDeposited: Int = 0,
    override var nukiCount: Int = 0
) : Player

class SanmaObservationEncoderTest : FunSpec({
    
    test("encoder should produce correct tensor shape") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            
            val tensor = encoder.encode(manager, observation, player)
            
            tensor.shape shouldBe Shape(11L, 27L)
            tensor.dataType shouldBe DataType.FLOAT32
        }
    }
    
    test("channel 0 should encode closed hand counts") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val channel0Data = FloatArray(27) { i -> tensor.getFloat(0, i.toLong()) }
            val sum = channel0Data.sum()
            
            sum shouldBeGreaterThanOrEqualTo 13f
            sum shouldBeLessThanOrEqualTo 14f
        }
    }
    
    test("channel 4 should mark aka dora (5p and 5s)") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val dora5p = tensor.getFloat(4, 9)
            dora5p shouldBe 1.0f
            
            val dora5s = tensor.getFloat(4, 18)
            dora5s shouldBe 1.0f
        }
    }
    
    test("channel 5-7 should encode wind and riichi state in first 3 indices") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val roundWindSum = (0 until 3).map { i -> tensor.getFloat(5, i.toLong()) }.sum()
            roundWindSum shouldBe 1.0f
            
            val roundWindRest = (3 until 27).map { i -> tensor.getFloat(5, i.toLong()) }.sum()
            roundWindRest shouldBe 0.0f
            
            val seatWindSum = (0 until 3).map { i -> tensor.getFloat(6, i.toLong()) }.sum()
            seatWindSum shouldBe 1.0f
        }
    }
    
    test("channel 10 should encode available actions") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val actionsSum = (0 until 11).map { i -> tensor.getFloat(10, i.toLong()) }.sum()
            actionsSum shouldBeGreaterThanOrEqualTo 1.0f
            
            val paddedSum = (11 until 27).map { i -> tensor.getFloat(10, i.toLong()) }.sum()
            paddedSum shouldBe 0.0f
        }
    }
    
    test("channel 9 should encode score difference") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 30000),
            TestPlayer(score = 25000),
            TestPlayer(score = 20000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val scoreValue = tensor.getFloat(9, 0)
            scoreValue shouldBeGreaterThan 0f
            
            for (i in 0 until 27) {
                tensor.getFloat(9, i.toLong()) shouldBe scoreValue
            }
        }
    }
    
    test("channel 8 should encode turn counter") {
        val encoder = SanmaObservationEncoder()
        val players = listOf(
            TestPlayer(score = 25000),
            TestPlayer(score = 25000),
            TestPlayer(score = 25000)
        )
        
        val matchResult = Match.create(
            ruleSet = RuleSet.RIICHI_SANMA_ARI_ARI,
            listeners = emptyList(),
            playerList = players,
            shuffleWinds = false
        )
        
        val match = (matchResult as Result.Success<Match>).value
        match.start()
        
        NDManager.newBaseManager().use { manager ->
            val observation = match.observation
            val player = players[0]
            val tensor = encoder.encode(manager, observation, player)
            
            val turnValue = tensor.getFloat(8, 0)
            turnValue shouldBeGreaterThanOrEqualTo 0f
            
            for (i in 0 until 27) {
                tensor.getFloat(8, i.toLong()) shouldBe turnValue
            }
        }
    }
})
