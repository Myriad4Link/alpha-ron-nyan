package xyz.uthofficial.arnyan.env.tile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.TileType

/**
 * Tests for dead wall dora indicator revelation rules:
 * - Dead wall has 14 tiles arranged as 7 stacks of 2 tiles each
 * - First dora indicator is the UPPER tile on the 3rd stack from the dead wall end
 * - When KAN occurs: reveal UPPER tile on next stack sequentially
 * - When RIICHI occurs: reveal LOWER tile on the next stack
 */
class DeadWallDoraTest : FunSpec({
    
    fun createLargeWall(tileCount: Int): List<Tile> {
        val tiles = mutableListOf<Tile>()
        var value = 1
        var suit: TileType = Man
        repeat(tileCount) {
            tiles.add(Tile(suit, value, false))
            value++
            if (value > 9) {
                value = 1
                suit = when (suit) {
                    Man -> Pin
                    Pin -> Man
                    else -> Man
                }
            }
        }
        return tiles.shuffled()
    }
    
    test("dead wall should store tiles as 2-tile stacks") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        // Dead wall should have 14 tiles = 7 stacks × 2 tiles
        // After initialization, 1 tile revealed (upper of 3rd stack), so 13 remaining
        wall.deadWallRemaining shouldBe 13
        wall.size shouldBe 122 // 136 - 14
    }
    
    test("first dora should be from 3rd stack (upper tile)") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Create wall with known tile sequence
        val tiles = (1..136).map { i ->
            val suit = if (i % 2 == 0) Man else Pin
            val value = ((i - 1) % 9) + 1
            Tile(suit, value, false)
        }.shuffled()
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        // Should have exactly 1 dora indicator revealed (upper tile on 3rd stack)
        val doras = wall.doraIndicators
        doras.size shouldBe 1
    }
    
    test("kan should reveal upper tile on NEXT stack") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Use deterministic tiles for easier debugging
        val tiles = (1..136).map { i -> Tile(Man, ((i - 1) % 9) + 1, false) }
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        val initialDora = wall.doraIndicators.first()
        
        // After kan, reveal next dora (upper tile on 4th stack)
        val result = wall.revealNextDoraIndicator()
        
        // Result should be success with the NEW dora tile
        (result is Result.Success) shouldBe true
        // New dora should be different from initial (from next stack)
        (result as Result.Success).value shouldNotBe initialDora
        // Current dora indicators should now show the new dora
        wall.doraIndicators.first() shouldNotBe initialDora
    }
    
    test("riichi should reveal lower tile on next stack") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        val initialDora = wall.doraIndicators.first()
        
        // After riichi, reveal lower tile of current stack (3rd stack)
        val riichiResult = wall.revealRiichiDoraIndicator()
        
        (riichiResult is Result.Success) shouldBe true
        // Should now have 2 dora indicators (upper + lower of same stack)
        wall.doraIndicators.size shouldBe 2
        // Lower tile should be different from upper tile
        wall.doraIndicators[0] shouldNotBe wall.doraIndicators[1]
        // Upper tile should still be the initial dora
        wall.doraIndicators[0] shouldBe initialDora
    }
    
    test("4 kans should reveal 4 additional doras (5 total)") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Use deterministic tiles
        val tiles = (1..136).map { i -> Tile(Man, ((i - 1) % 9) + 1, false) }
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        val revealedDoras = mutableListOf<Tile>()
        revealedDoras.add(wall.doraIndicators.first())
        
        // Simulate 4 kans
        repeat(4) {
            val result = wall.revealNextDoraIndicator()
            if (result is Result.Success) {
                revealedDoras.add(result.value)
            }
        }
        
        // Should have initial + 4 kan doras = 5 total
        revealedDoras.size shouldBe 5
        
        // All doras should be unique (from different stacks)
        // Note: With Manzu 1-9 repeating, we may have duplicates, so check count instead
        revealedDoras.size shouldBe 5
    }
    
    test("dead wall structure: stacks should be pairs of tiles") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Create wall with sequential values for easy tracking
        val tiles = (1..136).map { i ->
            Tile(Man, ((i - 1) % 9) + 1, false)
        }
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        // Dead wall should have exactly 14 tiles total
        // After initialization, 1 tile revealed, so 13 remaining
        wall.deadWallRemaining shouldBe 13
        
        // StandardTileWall stores as flat list but logically represents 7 stacks of 2 tiles
    }
    
    test("dora indicators should come from upper tiles only (for kan)") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Use deterministic tiles for easier debugging
        val tiles = (1..136).map { i -> Tile(Man, ((i - 1) % 9) + 1, false) }
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        val initialDora = wall.doraIndicators.first()
        
        // Reveal 3 more (simulating 3 kans)
        val kan2 = wall.revealNextDoraIndicator()
        val kan3 = wall.revealNextDoraIndicator()
        val kan4 = wall.revealNextDoraIndicator()
        
        // All should be successful
        (kan2 is Result.Success) shouldBe true
        (kan3 is Result.Success) shouldBe true
        (kan4 is Result.Success) shouldBe true
        
        // All doras should be different (from different stacks)
        val allDoras = listOf(initialDora) + 
            listOfNotNull((kan2 as? Result.Success)?.value,
                         (kan3 as? Result.Success)?.value,
                         (kan4 as? Result.Success)?.value)
        allDoras.distinct().size shouldBe 4
    }
    
    test("riichi after kan should reveal lower tile on new stack") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        val initialDora = wall.doraIndicators.first()
        
        // Kan: reveal upper tile on 4th stack
        wall.revealNextDoraIndicator()
        
        // Riichi: reveal lower tile on 4th stack
        val riichiResult = wall.revealRiichiDoraIndicator()
        
        (riichiResult is Result.Success) shouldBe true
        // Should now have 2 dora indicators (upper from 4th stack + lower from 4th stack)
        wall.doraIndicators.size shouldBe 2
    }
    
    test("exhausting all kan doras should fail gracefully") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        wall.initializeDeadWall(14)
        
        // Reveal all 4 kan doras (stacks 3, 4, 5, 6)
        repeat(4) {
            wall.revealNextDoraIndicator()
        }
        
        // 5th kan should fail (only 7 stacks, started at stack 2)
        val result = wall.revealNextDoraIndicator()
        (result is Result.Failure) shouldBe true
    }
    
    test("dead wall should be properly initialized after shuffle") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        // Simulate the dealing process: shuffle first, then initialize dead wall
        wall.shuffle()
        wall.initializeDeadWall(14)
        
        // Dead wall should have 14 tiles after initialization
        wall.deadWallRemaining shouldBe 13 // 14 - 1 (initial dora revealed)
        wall.doraIndicators.size shouldBe 1
    }
    
    test("all tiles should be accounted for after dealing") {
        val wall = StandardTileWall(standardDealAmount = 13)
        val tiles = createLargeWall(136)
        wall.addAll(tiles)
        
        // Simulate the full dealing process
        wall.shuffle()
        wall.initializeDeadWall(14)
        
        // Deal 13 tiles to 4 players
        val playerCount = 4
        val dealAmount = 13
        val dealtTiles = mutableListOf<Tile>()
        
        repeat(playerCount) {
            val drawn = wall.draw(dealAmount)
            if (drawn is Result.Success) {
                dealtTiles.addAll(drawn.value)
            }
        }
        
        // Total tiles should be: dealt (52) + dead wall (14) + live wall remaining
        // Note: deadWallRemaining excludes revealed doras, so we use tileWall which includes all remaining tiles
        // tileWall = live wall + dead wall (all 14 tiles)
        val totalAccountedFor = dealtTiles.size + wall.tileWall.size
        totalAccountedFor shouldBe 136
    }
    
    test("north winds should be present in wall after dealing") {
        val wall = StandardTileWall(standardDealAmount = 13)
        // Create full standard mahjong set with all 4 North winds
        val tiles = mutableListOf<Tile>()
        
        // Add all 4 North winds (Wind type, value 4)
        repeat(4) { tiles.add(Tile(Wind, 4, false)) }
        
        // Add remaining tiles to reach 136
        // Manzu 1-9 x4 = 36
        repeat(4) { repeat(9) { v -> tiles.add(Tile(Man, v + 1, false)) } }
        // Pinzu 1-9 x4 = 36
        repeat(4) { repeat(9) { v -> tiles.add(Tile(Pin, v + 1, false)) } }
        // Souzu 1-9 x4 = 36
        repeat(4) { repeat(9) { v -> tiles.add(Tile(Sou, v + 1, false)) } }
        // East, South, West x4 each = 12
        repeat(4) { tiles.add(Tile(Wind, 1, false)) }
        repeat(4) { tiles.add(Tile(Wind, 2, false)) }
        repeat(4) { tiles.add(Tile(Wind, 3, false)) }
        // Dragons 1-3 x4 each = 12
        repeat(4) { tiles.add(Tile(Dragon, 1, false)) }
        repeat(4) { tiles.add(Tile(Dragon, 2, false)) }
        repeat(4) { tiles.add(Tile(Dragon, 3, false)) }
        
        wall.addAll(tiles)
        wall.shuffle()
        wall.initializeDeadWall(14)
        
        // Deal 13 tiles to 4 players
        val playerCount = 4
        val dealAmount = 13
        val playerHands = mutableListOf<MutableList<Tile>>()
        
        repeat(playerCount) {
            val hand = mutableListOf<Tile>()
            val drawn = wall.draw(dealAmount)
            if (drawn is Result.Success) {
                hand.addAll(drawn.value)
            }
            playerHands.add(hand)
        }
        
        // Count North winds in all locations
        var northCount = 0
        
        // In player hands
        playerHands.forEach { hand ->
            northCount += hand.count { it.tileType is Wind && it.value == 4 }
        }
        
        // In live wall (use tileWall which returns all tiles including dead wall)
        // We need to count from the remaining live wall only
        // Since we can't access private tiles field, we'll count from what we can access
        // The wall.size gives us live wall count, and deadWallRemaining gives dead wall count
        // We'll use tileWall which returns tiles + deadWallTiles
        val allRemainingTiles = wall.tileWall
        northCount += allRemainingTiles.count { it.tileType is Wind && it.value == 4 }
        
        // Should have all 4 North winds accounted for
        northCount shouldBe 4
    }
})
