package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu

class StandardKantsuStrategyTest : FunSpec({
    val registry = TileTypeRegistry
    
    fun histogramOf(vararg tiles: Tile): IntArray {
        return registry.getHistogram(tiles.toList())
    }
    
    fun segmentStart(tileType: xyz.uthofficial.arnyan.env.tile.TileType): Int {
        return registry.getSegment(tileType)[0]
    }
    
    test("tryRemove should return true and decrement histogram for valid kantsu") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("tryRemove should return false when insufficient tiles in histogram") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe false
        histogram[index] shouldBe 3
    }
    
    test("tryRemove should return true for kantsu with more than 4 copies") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 1
    }
    
    test("tryRemove should work for different tile types and values") {
        val histogram = histogramOf(
            Tile(Sou, 5), Tile(Sou, 5), Tile(Sou, 5), Tile(Sou, 5)
        )
        val souStart = segmentStart(Sou)
        val index = souStart + 4
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("tryRemove should work for honor tiles (Dragon)") {
        val histogram = histogramOf(
            Tile(Dragon, 1), Tile(Dragon, 1), Tile(Dragon, 1), Tile(Dragon, 1)
        )
        val index = segmentStart(Dragon) + 0
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("tryRemove should work for honor tiles (Wind)") {
        val histogram = histogramOf(
            Tile(Wind, 1), Tile(Wind, 1), Tile(Wind, 1), Tile(Wind, 1)
        )
        val index = segmentStart(Wind) + 0
        
        val result = StandardKantsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("revert should restore histogram after successful tryRemove") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val removed = StandardKantsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 0
        
        StandardKantsuStrategy.revert(histogram, index)
        
        histogram[index] shouldBe 4
    }
    
    test("revert should handle multiple increments correctly after partial removal") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val removed = StandardKantsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 2
        
        StandardKantsuStrategy.revert(histogram, index)
        
        histogram[index] shouldBe 6
    }
    
    test("type property should return Kantsu") {
        StandardKantsuStrategy.type shouldBe Kantsu
    }
})