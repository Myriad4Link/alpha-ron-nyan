package xyz.uthofficial.arnyan.env.yaku.resolver

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
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

    fun isTenpaiMentsus(mentsus: LongArray): Boolean {
        val validMentsuTypes = setOf(Shuntsu, Koutsu, Kantsu)
        when (mentsus.size) {
            1 -> {
                return CompactMentsu(mentsus[0]).mentsuType === Kokushi
            }
            5 -> {
                val toitsuCount = mentsus.count { CompactMentsu(it).mentsuType === Toitsu }
                if (toitsuCount != 1) return false
                return mentsus.all { mentsu ->
                    val type = CompactMentsu(mentsu).mentsuType
                    type === Toitsu || type in validMentsuTypes
                }
            }

            7 -> {
                return mentsus.all { CompactMentsu(it).mentsuType === Toitsu }
            }

            else -> return false
        }
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

        // Should have two waiting tiles: 1p and 4s
        result.keys.size shouldBe 2
        val pin1Index = indexOf(Tile(Pin, 1))
        val sou4Index = indexOf(Tile(Sou, 4))
        result shouldContainKey pin1Index
        result shouldContainKey sou4Index

        // Verify each waiting tile has at least one winning configuration
        result[pin1Index]!!.isNotEmpty() shouldBe true
        result[sou4Index]!!.isNotEmpty() shouldBe true

        // For each configuration, verify it's a valid winning hand
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

        // Chiitoitsu: 6 pairs + 1 single tile
        // Hand: 11m 22m 33m 44m 55m 66m 7m (single)
        // Waiting for 7m to complete pair
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

        // Should have exactly one waiting tile: 7m
        val man7Index = indexOf(Tile(Man, 7))

        // For now, just verify that 7m is a waiting tile with chiitoitsu configuration
        result shouldContainKey man7Index
        val configs = result[man7Index]!!
        configs.isNotEmpty() shouldBe true

        // At least one configuration should be chiitoitsu
        val chiitoitsuConfigs = configs.filter { config ->
            config.size == 7 && config.all { CompactMentsu(it).mentsuType === Toitsu }
        }
        chiitoitsuConfigs.isNotEmpty() shouldBe true

        // Also check that all configurations for 7m are valid winning hands
        for (config in configs) {
            val isValid = isTenpaiMentsus(config)
            isValid shouldBe true
        }

        // Note: hand may also be tenpai for other tiles via standard patterns
        // That's acceptable
    }





    test("hand not in tenpai returns empty map") {
        val resolver = StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
        val evaluator = StandardFastTenpaiEvaluator(resolver)

        // Random hand unlikely to be in tenpai
        // Use 13 distinct tiles (no pairs, no sequences)
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