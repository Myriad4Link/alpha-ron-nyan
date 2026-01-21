package xyz.uthofficial.arnyan.env.yaku.resolver.strategies

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu

class StandardShuntsuStrategyTest : FunSpec({
    val registry = TileTypeRegistry

    fun histogramOf(vararg tiles: Tile): IntArray {
        return registry.getHistogram(tiles.toList())
    }

    fun segmentStart(tileType: xyz.uthofficial.arnyan.env.tile.TileType): Int {
        return registry.getSegment(tileType)[0]
    }

    test("tryRemove should return true and decrement histogram for valid shuntsu") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 2), Tile(Man, 3)
        )
        val index = segmentStart(Man) + 0 // value 1 index

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe true
        histogram[index] shouldBe 0
        histogram[index + 1] shouldBe 0
        histogram[index + 2] shouldBe 0
    }

    test("tryRemove should return false when mask negative (not-continuous tile type)") {
        val histogram = histogramOf(
            Tile(Wind, 1), Tile(Wind, 2), Tile(Wind, 3)
        )
        val index = segmentStart(Wind) + 0

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe false
        histogram[index] shouldBe 1
        histogram[index + 1] shouldBe 1
        histogram[index + 2] shouldBe 1
    }

    test("tryRemove should return false when mask mismatch across indices") {
        val histogram = IntArray(registry.connectivityMask.size)
        val manStart = segmentStart(Man)
        val pinStart = segmentStart(Pin)

        histogram[manStart + 7] = 1
        histogram[manStart + 8] = 1
        histogram[pinStart] = 1
        val index = manStart + 7

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe false
        histogram[manStart + 7] shouldBe 1
        histogram[manStart + 8] shouldBe 1
        histogram[pinStart] shouldBe 1
    }

    test("tryRemove should return false when insufficient tiles in histogram") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 2)
        )
        val index = segmentStart(Man) + 0

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe false
        histogram[index] shouldBe 1
        histogram[index + 1] shouldBe 1
        histogram[index + 2] shouldBe 0
    }

    test("tryRemove should return false when index out of bounds (index > histogram.size - 3)") {
        val histogram = histogramOf() // empty histogram size = total distinct values
        val index = histogram.size - 2 // index where index+2 would be out of bounds

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe false
    }

    test("revert should restore histogram after successful tryRemove") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 2), Tile(Man, 3)
        )
        val index = segmentStart(Man) + 0

        val removed = StandardShuntsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 0
        histogram[index + 1] shouldBe 0
        histogram[index + 2] shouldBe 0

        StandardShuntsuStrategy.revert(histogram, index)

        histogram[index] shouldBe 1
        histogram[index + 1] shouldBe 1
        histogram[index + 2] shouldBe 1
    }

    test("revert should handle multiple increments correctly") {
        val histogram = histogramOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 2), Tile(Man, 2), Tile(Man, 3), Tile(Man, 3)
        )
        val index = segmentStart(Man) + 0

        val removed = StandardShuntsuStrategy.tryRemove(histogram, index)
        removed shouldBe true
        histogram[index] shouldBe 1
        histogram[index + 1] shouldBe 1
        histogram[index + 2] shouldBe 1

        StandardShuntsuStrategy.revert(histogram, index)

        histogram[index] shouldBe 2
        histogram[index + 1] shouldBe 2
        histogram[index + 2] shouldBe 2
    }

    test("tryRemove should work for shuntsu at different positions within segment") {
        val histogram = histogramOf(
            Tile(Sou, 4), Tile(Sou, 5), Tile(Sou, 6)
        )
        val souStart = segmentStart(Sou)
        val index = souStart + 3

        val result = StandardShuntsuStrategy.tryRemove(histogram, index)

        result shouldBe true
        histogram[index] shouldBe 0
        histogram[index + 1] shouldBe 0
        histogram[index + 2] shouldBe 0
    }

    test("type property should return Shuntsu") {
        StandardShuntsuStrategy.type shouldBe Shuntsu
    }
})