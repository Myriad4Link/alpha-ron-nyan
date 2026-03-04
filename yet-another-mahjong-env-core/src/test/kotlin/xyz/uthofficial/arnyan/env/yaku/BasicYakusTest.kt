package xyz.uthofficial.arnyan.env.yaku

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu

class BasicYakusTest : FunSpec({
    // Helper to compute tile index (Man 1 = 3, Man 2 = 4, ..., Man 9 = 11)
    fun manIndex(value: Int): Int = value + 2
    fun pinIndex(value: Int): Int = value + 11
    fun souIndex(value: Int): Int = value + 20
    fun dragonIndex(value: Int): Int = value - 1  // 1->0, 2->1, 3->2
    fun windIndex(value: Int): Int = value + 29   // 1->30 (East), 2->31, 3->32, 4->33

    fun packMentsu(tileIndices: IntArray, mentsuType: String, isOpen: Boolean = false): Long {
        val typeIndex = when (mentsuType) {
            "Kantsu" -> MentsuTypeRegistry.getIndex(Kantsu)
            "Koutsu" -> MentsuTypeRegistry.getIndex(Koutsu)
            "Shuntsu" -> MentsuTypeRegistry.getIndex(Shuntsu)
            "Toitsu" -> MentsuTypeRegistry.getIndex(Toitsu)
            else -> error("Unknown mentsu type")
        }
        val containsYaochuhai = tileIndices.any { TileTypeRegistry.yaochuhaiIndices.contains(it) }
        return CompactMentsu.pack(tileIndices, typeIndex, isOpen, containsYaochuhai)
    }

    context("Tanyao") {
        test("should return [1] when no mentsu contains yaochuhai") {
            // Three shuntsus of simples + one toitsu of simple
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(souIndex(2), souIndex(2)), "Toitsu"),
            )
            val result = Tanyao.judge(partition, null)
            result shouldBe intArrayOf(1)
        }

        test("should return empty when any mentsu contains yaochuhai") {
            // One shuntsu with terminal (Man1)
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(1), manIndex(2), manIndex(3)), "Shuntsu"), // Man1 is yaochuhai
                packMentsu(intArrayOf(manIndex(4), manIndex(5), manIndex(6)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(souIndex(2), souIndex(2)), "Toitsu"),
            )
            val result = Tanyao.judge(partition, null)
            result shouldBe intArrayOf()
        }
    }

    context("Yakuhai") {
        test("should count dragon koutsu") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(dragonIndex(1), dragonIndex(1), dragonIndex(1)), "Koutsu"),
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Yakuhai.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should count seat wind koutsu") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(windIndex(1), windIndex(1), windIndex(1)), "Koutsu"), // East wind
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.SOUTH,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Yakuhai.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should count round wind koutsu") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(windIndex(2), windIndex(2), windIndex(2)), "Koutsu"), // South wind
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.SOUTH,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Yakuhai.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should not count non-yakuhai wind koutsu") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(windIndex(3), windIndex(3), windIndex(3)), "Koutsu"), // West wind
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.SOUTH,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Yakuhai.judge(partition, context)
            result shouldBe intArrayOf()
        }

        test("should count multiple yakuhai") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(dragonIndex(1), dragonIndex(1), dragonIndex(1)), "Koutsu"),
                packMentsu(intArrayOf(windIndex(1), windIndex(1), windIndex(1)), "Koutsu"),
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Yakuhai.judge(partition, context)
            result shouldBe intArrayOf(1, 1)
        }
    }

    context("MenzenTsumo") {
        test("should detect closed hand self-draw") {
            val partition = longArrayOf() // mentsus irrelevant
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.TSUMO
            )
            val result = MenzenTsumo.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should not detect open hand") {
            val partition = longArrayOf()
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = true,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.TSUMO
            )
            val result = MenzenTsumo.judge(partition, context)
            result shouldBe intArrayOf()
        }

        test("should not detect ron") {
            val partition = longArrayOf()
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = MenzenTsumo.judge(partition, context)
            result shouldBe intArrayOf()
        }
    }

    context("Riichi") {
        test("should detect declared riichi") {
            val partition = longArrayOf()
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = true,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Riichi.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should not detect when riichi not declared") {
            val partition = longArrayOf()
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Riichi.judge(partition, context)
            result shouldBe intArrayOf()
        }

        test("should return empty when context is null") {
            val partition = longArrayOf()
            val result = Riichi.judge(partition, null)
            result shouldBe intArrayOf()
        }
    }

    context("Pinfu") {
        test("should detect pinfu with all shuntsu and simple pair") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(souIndex(2), souIndex(2)), "Toitsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Pinfu.judge(partition, context)
            result shouldBe intArrayOf(1)
        }

        test("should reject open hand") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(souIndex(2), souIndex(2)), "Toitsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = true,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Pinfu.judge(partition, context)
            result shouldBe intArrayOf()
        }

        test("should reject yaochuhai pair") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(6), pinIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(dragonIndex(1), dragonIndex(1)), "Toitsu"), // dragon pair
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Pinfu.judge(partition, context)
            result shouldBe intArrayOf()
        }

        test("should reject hand with koutsu") {
            val partition = longArrayOf(
                packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(manIndex(5), manIndex(6), manIndex(7)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), "Shuntsu"),
                packMentsu(intArrayOf(pinIndex(5), pinIndex(5), pinIndex(5)), "Koutsu"),
                packMentsu(intArrayOf(souIndex(2), souIndex(2)), "Toitsu"),
            )
            val context = YakuContext(
                seatWind = StandardWind.EAST,
                roundWind = StandardWind.EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 2, false),
                winningMethod = WinningMethod.RON
            )
            val result = Pinfu.judge(partition, context)
            result shouldBe intArrayOf()
        }
    }
})