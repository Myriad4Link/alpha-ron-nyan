package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu

class StandardKoutsuStrategyTest : FunSpec({
    val registry = TileTypeRegistry
    
    fun histogramOf(vararg tiles: Tile): IntArray {
        return registry.getHistogram(tiles.toList())
    }
    
    fun segmentStart(tileType: xyz.uthofficial.arnyan.env.tile.TileType): Int {
        return registry.getSegment(tileType)[0]
    }
    
    test("tryRemove should return true and decrement histogram for valid koutsu") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKoutsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("tryRemove should return false when insufficient tiles in histogram") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKoutsuStrategy.tryRemove(histogram, index)
        
        result shouldBe false
        histogram[index] shouldBe 2
    }
    
    test("tryRemove should return true for koutsu with more than 3 copies") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val result = StandardKoutsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 1
    }
    
    test("tryRemove should work for different tile types and values") {
        val histogram = histogramOf(
            Tile(Sou, 5), Tile(Sou, 5), Tile(Sou, 5)
        )
        val souStart = segmentStart(Sou)
        val index = souStart + 4
        
        val result = StandardKoutsuStrategy.tryRemove(histogram, index)
        
        result shouldBe true
        histogram[index] shouldBe 0
    }
    
    test("revert should restore histogram after successful tryRemove") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val removed = StandardKoutsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 0
        
        StandardKoutsuStrategy.revert(histogram, index)
        
        histogram[index] shouldBe 3
    }
    
    test("revert should handle multiple increments correctly after partial removal") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1), Tile(Man, 1)
        )
        val index = segmentStart(Man) + 0
        
        val removed = StandardKoutsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 2
        
        StandardKoutsuStrategy.revert(histogram, index)
        
        histogram[index] shouldBe 5
    }
    
    test("type property should return Koutsu") {
        StandardKoutsuStrategy.type shouldBe Koutsu
    }
})