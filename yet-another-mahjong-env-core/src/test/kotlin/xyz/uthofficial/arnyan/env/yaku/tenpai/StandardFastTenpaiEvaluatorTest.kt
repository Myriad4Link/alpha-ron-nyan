package xyz.uthofficial.arnyan.env.yaku.tenpai

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu
import xyz.uthofficial.arnyan.env.yaku.resolver.StandardFastTileResolver
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKantsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKoutsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardShuntsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardToitsuStrategy

class StandardFastTenpaiEvaluatorTest : FunSpec({

    fun histogramOf(vararg tiles: Tile): IntArray {
        val histogram = IntArray(TileTypeRegistry.SIZE)
        TileTypeRegistry.getHistogram(tiles.toList(), histogram)
        return histogram
    }

    fun indexOf(tile: Tile): Int {
        val histogram = IntArray(TileTypeRegistry.SIZE)
        TileTypeRegistry.getHistogram(listOf(tile), histogram)
        return histogram.indexOfFirst { it > 0 }
    }



    test("empty histogram returns empty map") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
        val evaluator = StandardFastTenpaiEvaluator(resolver)
        val histogram = IntArray(TileTypeRegistry.SIZE)

        val result = evaluator.evaluate(histogram)
        result shouldBe emptyMap()
    }

    test("hand with standard tenpai - waiting for single tile to complete triplet - should be correctly processed") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
        val evaluator = StandardFastTenpaiEvaluator(resolver)

        val hand = listOf(
            Tile(Man, 1), Tile(Man, 1), Tile(Man, 1),
            Tile(Man, 2), Tile(Man, 2), Tile(Man, 2),
            Tile(Man, 3), Tile(Man, 3), Tile(Man, 3),
            Tile(Pin, 1), Tile(Pin, 1),
            Tile(Sou, 4), Tile(Sou, 4)
        )
        val histogram = histogramOf(*hand.toTypedArray())

        val result = evaluator.evaluate(histogram)

        result.keys.size shouldBe 2
        val pin1Index = indexOf(Tile(Pin, 1))
        val sou4Index = indexOf(Tile(Sou, 4))
        result shouldContainKey pin1Index
        result shouldContainKey sou4Index

        result[pin1Index]!!.isNotEmpty() shouldBe true
        result[sou4Index]!!.isNotEmpty() shouldBe true

        for ((_, configs) in result) {
            for (config in configs) {
                config.size shouldBe 5
                val toitsuCount = config.count { CompactMentsu(it).mentsuType === Toitsu }
                toitsuCount shouldBe 1
            }
        }
    }

    test("hand with chiitoitsu tenpai - waiting for pair") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
        val evaluator = StandardFastTenpaiEvaluator(resolver)

        val hand = listOf(
            Tile(Man, 1), Tile(Man, 1),
            Tile(Man, 2), Tile(Man, 2),
            Tile(Man, 3), Tile(Man, 3),
            Tile(Man, 4), Tile(Man, 4),
            Tile(Man, 5), Tile(Man, 5),
            Tile(Man, 6), Tile(Man, 6),
            Tile(Man, 7)
        )
        val histogram = histogramOf(*hand.toTypedArray())

        val result = evaluator.evaluate(histogram)

        val man7Index = indexOf(Tile(Man, 7))

        result shouldContainKey man7Index
        val configs = result[man7Index]!!
        configs.isNotEmpty() shouldBe true

        val chiitoitsuConfigs = configs.filter { config ->
            config.size == 7 && config.all { CompactMentsu(it).mentsuType === Toitsu }
        }
        chiitoitsuConfigs.isNotEmpty() shouldBe true

        for (config in configs) {
            val isValid = evaluator.isTenpaiMentsus(config)
            isValid shouldBe true
        }
    }





    test("hand not in tenpai returns empty map") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
        val evaluator = StandardFastTenpaiEvaluator(resolver)

        val hand = listOf(
            Tile(Man, 1), Tile(Man, 2), Tile(Man, 3), Tile(Man, 4), Tile(Man, 5),
            Tile(Pin, 1), Tile(Pin, 2), Tile(Pin, 3), Tile(Pin, 4), Tile(Pin, 5),
            Tile(Sou, 1), Tile(Sou, 2), Tile(Sou, 3)
        )
        val histogram = histogramOf(*hand.toTypedArray())

        val result = evaluator.evaluate(histogram)

        result shouldBe emptyMap()
    }
})