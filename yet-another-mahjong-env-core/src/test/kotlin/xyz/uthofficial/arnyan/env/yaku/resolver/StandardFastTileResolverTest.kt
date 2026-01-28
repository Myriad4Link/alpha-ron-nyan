package xyz.uthofficial.arnyan.env.yaku.resolver

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKantsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKoutsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardShuntsuStrategy

class StandardFastTileResolverTest : FunSpec({
    val registry = TileTypeRegistry
    
    fun handOf(vararg tiles: Tile): List<Tile> {
        return tiles.toList()
    }

    test("empty hand returns list containing empty list") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val result = resolver.resolve(emptyList())
        
        result shouldBe listOf(emptyList())
    }
    
    test("single tile returns empty list") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(Tile(Man, 1))
        val result = resolver.resolve(hand)
        
        result shouldBe emptyList()
    }
    
    test("three identical man tiles resolves to Koutsu") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1),
            Tile(Man, 1),
            Tile(Man, 1)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Koutsu))
    }
    
    test("three consecutive man tiles resolves to Shuntsu") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1),
            Tile(Man, 2),
            Tile(Man, 3)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Shuntsu))
    }
    
    test("four identical man tiles resolves to Kantsu") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1),
            Tile(Man, 1),
            Tile(Man, 1),
            Tile(Man, 1)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Kantsu))
    }
    
    test("mixed hand with multiple possible partitions returns all partitions") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1),
            Tile(Man, 2), Tile(Man, 2), Tile(Man, 2),
            Tile(Man, 3), Tile(Man, 3), Tile(Man, 3)
        )
        val result = resolver.resolve(hand)
        
        val expectedPartitions = listOf(
            listOf(Shuntsu, Shuntsu, Shuntsu),
            listOf(Koutsu, Koutsu, Koutsu)
        ).map { it.sortedBy { it::class.simpleName } }
        
        val sortedResult = result.map { it.sortedBy { it::class.simpleName } }
        
        sortedResult shouldContainAll expectedPartitions
    }
    
    test("hand with multiple tile types works correctly") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1),
            Tile(Sou, 5), Tile(Sou, 5), Tile(Sou, 5)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Koutsu, Koutsu))
    }
    
    test("hand with wind tiles cannot form shuntsu") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Wind, 1),
            Tile(Wind, 1),
            Tile(Wind, 1)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Koutsu))
    }
    
    test("hand with insufficient tiles for any mentsu returns empty") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1),
            Tile(Man, 2)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe emptyList()
    }
    
    test("hand with dragon tiles works correctly") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Dragon, 1),
            Tile(Dragon, 1),
            Tile(Dragon, 1)
        )
        val result = resolver.resolve(hand)
        
        result shouldBe listOf(listOf(Koutsu))
    }
    
    test("complex hand with mixed patterns returns all valid partitions") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy
        )
        
        val hand = handOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1),
            Tile(Man, 2), Tile(Man, 2), Tile(Man, 2),
            Tile(Man, 3), Tile(Man, 3), Tile(Man, 3)
        )
        val result = resolver.resolve(hand)
        
        result shouldContain listOf(Koutsu, Koutsu, Koutsu)
    }
})