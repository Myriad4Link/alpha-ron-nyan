package xyz.uthofficial.arnyan.env.scoring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.wind.Wind as SeatWind
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.yaku.resolver.*
import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

class StandardScoringCalculatorTest : FunSpec({
    val calculator = StandardScoringCalculator()

    fun manIndex(value: Int): Int = value + 2
    fun pinIndex(value: Int): Int = value + 11
    fun souIndex(value: Int): Int = value + 20
    fun dragonIndex(value: Int): Int = value - 1
    fun windIndex(value: Int): Int = value + 29

    fun packMentsu(tileIndices: IntArray, mentsuType: MentsuType, isOpen: Boolean = false): Long {
        val typeIndex = MentsuTypeRegistry.getIndex(mentsuType)
        val containsYaochuhai = tileIndices.any { TileTypeRegistry.yaochuhaiIndices.contains(it) }
        return CompactMentsu.pack(tileIndices, typeIndex, isOpen, containsYaochuhai)
    }

    fun createYakuContext(
        seatWind: StandardWind = StandardWind.EAST,
        roundWind: StandardWind = StandardWind.EAST,
        isOpen: Boolean = false,
        isRiichi: Boolean = false,
        winningTile: Tile,
        method: WinningMethod = WinningMethod.RON
    ): YakuContext = YakuContext(
        seatWind = seatWind,
        roundWind = roundWind,
        isOpenHand = isOpen,
        isRiichiDeclared = isRiichi,
        winningTile = winningTile,
        winningMethod = method
    )

    context("calculateFu") {
        context("base fu") {
            test("open hand base fu is 20") {
                val tiles = listOf(
                    Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Man, 3, false), Tile(Man, 4, false), Tile(Man, 5, false),
                    Tile(Pin, 3, false), Tile(Pin, 4, false), Tile(Pin, 5, false),
                    Tile(Sou, 3, false), Tile(Sou, 4, false), Tile(Sou, 5, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(3), manIndex(4), manIndex(5)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(3), pinIndex(4), pinIndex(5)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(3), souIndex(4), souIndex(5)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu, isOpen = true))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 7, false), isOpen = true)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 30
            }

            test("closed hand base fu is 30") {
                val tiles = listOf(
                    Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Man, 3, false), Tile(Man, 4, false), Tile(Man, 5, false),
                    Tile(Pin, 3, false), Tile(Pin, 4, false), Tile(Pin, 5, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(3), manIndex(4), manIndex(5)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(3), pinIndex(4), pinIndex(5)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 5, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 30
            }
        }

        context("yakuhai pair fu") {
            test("dragon pair adds 2 fu") {
                val tiles = listOf(
                    Tile(Dragon, 1, false), Tile(Dragon, 1, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(dragonIndex(1), dragonIndex(1)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("seat wind pair adds 2 fu") {
                val tiles = listOf(
                    Tile(Wind, 1, false), Tile(Wind, 1, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(windIndex(1), windIndex(1)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(
                    seatWind = StandardWind.EAST,
                    winningTile = Tile(Sou, 6, false),
                    isOpen = false
                )
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("round wind pair adds 2 fu") {
                val tiles = listOf(
                    Tile(Wind, 2, false), Tile(Wind, 2, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(windIndex(2), windIndex(2)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(
                    roundWind = StandardWind.SOUTH,
                    winningTile = Tile(Sou, 6, false),
                    isOpen = false
                )
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("non yakuhai pair adds 0 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }
        }

        context("koutsu fu") {
            test("open simple koutsu adds 4 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2), manIndex(2)), Koutsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = true)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 30
            }

            test("closed simple koutsu adds 8 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2), manIndex(2)), Koutsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("open terminal koutsu adds 4 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(1), manIndex(1), manIndex(1)), Koutsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = true)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 30
            }

            test("closed terminal koutsu adds 8 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(1), manIndex(1), manIndex(1)), Koutsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }
        }

        context("kantsu fu") {
            test("open kantsu adds 16 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2), manIndex(2), manIndex(2)), Kantsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = true)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("closed kantsu adds 32 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2), manIndex(2), manIndex(2)), Kantsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 5, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 70
            }
        }

        context("wait fu") {
            test("tanki wait (pair completion) adds 2 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Man, 5, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("kanchan wait (middle) adds 2 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("penchan wait (edge) adds 2 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 1, false), Tile(Sou, 2, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(1), souIndex(2), souIndex(3)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 3, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }
        }

        context("menzen tsumo fu") {
            test("closed hand tsumo adds 2 fu") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 7, false), isOpen = false, method = WinningMethod.TSUMO)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("closed hand ron adds 0 fu (base only)") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = false, method = WinningMethod.RON)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }

            test("open hand tsumo adds 0 fu (base only)") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 3, false), Tile(Man, 4, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 5, false), Tile(Sou, 6, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(3), manIndex(4)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu, isOpen = true)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(4), souIndex(5), souIndex(6)), Shuntsu, isOpen = true))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 6, false), isOpen = true, method = WinningMethod.TSUMO)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 30
            }
        }

        context("fu rounding") {
            test("rounds up to nearest 10") {
                val tiles = listOf(
                    Tile(Man, 5, false), Tile(Man, 5, false),
                    Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                    Tile(Pin, 2, false), Tile(Pin, 3, false), Tile(Pin, 4, false),
                    Tile(Sou, 2, false), Tile(Sou, 3, false), Tile(Sou, 4, false),
                    Tile(Sou, 6, false), Tile(Sou, 7, false)
                )
                val mentsus = listOf(
                    CompactMentsu(packMentsu(intArrayOf(manIndex(5), manIndex(5)), Toitsu)),
                    CompactMentsu(packMentsu(intArrayOf(manIndex(2), manIndex(2), manIndex(2)), Koutsu)),
                    CompactMentsu(packMentsu(intArrayOf(pinIndex(2), pinIndex(3), pinIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(2), souIndex(3), souIndex(4)), Shuntsu)),
                    CompactMentsu(packMentsu(intArrayOf(souIndex(5), souIndex(6), souIndex(7)), Shuntsu))
                )
                val context = createYakuContext(winningTile = Tile(Sou, 7, false), isOpen = false)
                val fu = calculator.calculateFu(tiles, mentsus, context, StandardWind.EAST, false)
                fu shouldBe 40
            }
        }
    }

    context("calculateBasicPoints") {
        context("limit hands") {
            test("mangan (5 han) returns 2000") {
                calculator.calculateBasicPoints(5, 30, false, false) shouldBe 2000
            }

            test("haneman (6 han) returns 3000") {
                calculator.calculateBasicPoints(6, 30, false, false) shouldBe 3000
            }

            test("haneman (7 han) returns 3000") {
                calculator.calculateBasicPoints(7, 30, false, false) shouldBe 3000
            }

            test("baiman (8 han) returns 4000") {
                calculator.calculateBasicPoints(8, 30, false, false) shouldBe 4000
            }

            test("baiman (10 han) returns 4000") {
                calculator.calculateBasicPoints(10, 30, false, false) shouldBe 4000
            }

            test("sanbaiman (11 han) returns 6000") {
                calculator.calculateBasicPoints(11, 30, false, false) shouldBe 6000
            }

            test("sanbaiman (12 han) returns 6000") {
                calculator.calculateBasicPoints(12, 30, false, false) shouldBe 6000
            }

            test("yakuman (13 han) returns 8000") {
                calculator.calculateBasicPoints(13, 30, false, false) shouldBe 8000
            }

            test("yakuman (26 han) returns 8000") {
                calculator.calculateBasicPoints(26, 30, false, false) shouldBe 8000
            }
        }

        context("basic points formula") {
            test("1 han 30 fu = 300") {
                calculator.calculateBasicPoints(1, 30, false, false) shouldBe 300
            }

            test("2 han 40 fu = 700") {
                calculator.calculateBasicPoints(2, 40, false, false) shouldBe 700
            }

            test("3 han 50 fu = 1600") {
                calculator.calculateBasicPoints(3, 50, false, false) shouldBe 1600
            }

            test("4 han 60 fu = 3900") {
                calculator.calculateBasicPoints(4, 60, false, false) shouldBe 3900
            }
        }
    }

    context("distributePayments") {
        val playerWinds = listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)

        context("ron payments (sanma)") {
            test("dealer wins by ron receives 6x basic points") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = false,
                    isDealer = true,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    playerWinds = playerWinds,
                    loserWind = StandardWind.SOUTH
                )
                payments[StandardWind.EAST] shouldBe 12000
                payments[StandardWind.SOUTH] shouldBe -12000
                payments[StandardWind.WEST] shouldBe 0
            }

            test("non dealer wins by ron receives 4x basic points") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.SOUTH,
                    basicPoints = 2000,
                    isTsumo = false,
                    isDealer = false,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    playerWinds = playerWinds,
                    loserWind = StandardWind.EAST
                )
                payments[StandardWind.SOUTH] shouldBe 8000
                payments[StandardWind.EAST] shouldBe -8000
                payments[StandardWind.WEST] shouldBe 0
            }

            test("ron with riichi sticks") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = false,
                    isDealer = true,
                    riichiSticks = 2,
                    honbaSticks = 0,
                    playerWinds = playerWinds,
                    loserWind = StandardWind.SOUTH
                )
                payments[StandardWind.EAST] shouldBe 14000
                payments[StandardWind.SOUTH] shouldBe -14000
            }

            test("ron with honba sticks") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = false,
                    isDealer = true,
                    riichiSticks = 0,
                    honbaSticks = 3,
                    playerWinds = playerWinds,
                    loserWind = StandardWind.SOUTH
                )
                payments[StandardWind.EAST] shouldBe 12300
                payments[StandardWind.SOUTH] shouldBe -12300
            }
        }

        context("tsumo payments (sanma)") {
            test("dealer wins by tsumo each non dealer pays 2x basic points") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = true,
                    isDealer = true,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    playerWinds = playerWinds
                )
                payments[StandardWind.EAST] shouldBe 8000
                payments[StandardWind.SOUTH] shouldBe -4000
                payments[StandardWind.WEST] shouldBe -4000
            }

            test("non dealer wins by tsumo dealer pays 2x others pay 1x") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.SOUTH,
                    basicPoints = 2000,
                    isTsumo = true,
                    isDealer = false,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    playerWinds = playerWinds
                )
                payments[StandardWind.SOUTH] shouldBe 6000
                payments[StandardWind.EAST] shouldBe -4000
                payments[StandardWind.WEST] shouldBe -2000
            }

            test("tsumo with riichi sticks") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = true,
                    isDealer = true,
                    riichiSticks = 3,
                    honbaSticks = 0,
                    playerWinds = playerWinds
                )
                payments[StandardWind.EAST] shouldBe 14000
                payments[StandardWind.SOUTH] shouldBe -7000
                payments[StandardWind.WEST] shouldBe -7000
            }

            test("tsumo with honba sticks") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.SOUTH,
                    basicPoints = 2000,
                    isTsumo = true,
                    isDealer = false,
                    riichiSticks = 0,
                    honbaSticks = 2,
                    playerWinds = playerWinds
                )
                payments[StandardWind.SOUTH] shouldBe 6400
                payments[StandardWind.EAST] shouldBe -4200
                payments[StandardWind.WEST] shouldBe -2200
            }
        }

        context("payment balance") {
            test("ron payments sum to zero") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.EAST,
                    basicPoints = 2000,
                    isTsumo = false,
                    isDealer = true,
                    riichiSticks = 2,
                    honbaSticks = 3,
                    playerWinds = playerWinds,
                    loserWind = StandardWind.SOUTH
                )
                payments.values.sum() shouldBe 0
            }

            test("tsumo payments sum to zero") {
                val payments = calculator.distributePayments(
                    winnerWind = StandardWind.SOUTH,
                    basicPoints = 2000,
                    isTsumo = true,
                    isDealer = false,
                    riichiSticks = 1,
                    honbaSticks = 2,
                    playerWinds = playerWinds
                )
                payments.values.sum() shouldBe 0
            }
        }
    }

    context("computeExhaustiveDrawPayments") {
        val playerWinds = listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)

        context("nagashi mangan detection") {
            test("single eligible player receives 2000 from each other") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 1, false), Tile(Man, 9, false), Tile(Pin, 1, false)),
                    StandardWind.SOUTH to listOf(Tile(Man, 5, false), Tile(Pin, 5, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 5, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to emptyList(),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments[StandardWind.EAST] shouldBe 4000
                payments[StandardWind.SOUTH] shouldBe -2000
                payments[StandardWind.WEST] shouldBe -2000
            }

            test("player with simple tile not eligible") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 1, false), Tile(Man, 5, false)),
                    StandardWind.SOUTH to listOf(Tile(Pin, 1, false), Tile(Pin, 5, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 1, false), Tile(Sou, 5, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to emptyList(),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments.values.all { it == 0 } shouldBe true
            }

            test("player with calls not eligible") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 1, false), Tile(Man, 9, false)),
                    StandardWind.SOUTH to listOf(Tile(Pin, 1, false), Tile(Pin, 9, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 1, false), Tile(Sou, 9, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to listOf(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false))),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments[StandardWind.EAST] shouldBe -4000
                payments[StandardWind.SOUTH] shouldBe 2000
                payments[StandardWind.WEST] shouldBe 2000
            }

            test("multiple eligible players pay each other net zero") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 1, false), Tile(Man, 9, false), Tile(xyz.uthofficial.arnyan.env.tile.Wind, 1, false)),
                    StandardWind.SOUTH to listOf(Tile(Pin, 1, false), Tile(Pin, 9, false), Tile(xyz.uthofficial.arnyan.env.tile.Wind, 2, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 5, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to emptyList(),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments[StandardWind.EAST] shouldBe 2000
                payments[StandardWind.SOUTH] shouldBe 2000
                payments[StandardWind.WEST] shouldBe -4000
            }

            test("no eligible players no payments") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 5, false)),
                    StandardWind.SOUTH to listOf(Tile(Pin, 5, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 5, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to emptyList(),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments.values.all { it == 0 } shouldBe true
            }
        }

        context("payment balance") {
            test("nagashi mangan payments sum to zero") {
                val discards = mapOf<SeatWind, List<Tile>>(
                    StandardWind.EAST to listOf(Tile(Man, 1, false), Tile(Man, 9, false)),
                    StandardWind.SOUTH to listOf(Tile(Pin, 5, false)),
                    StandardWind.WEST to listOf(Tile(Sou, 5, false))
                )
                val openHands = mapOf<SeatWind, List<List<Tile>>>(
                    StandardWind.EAST to emptyList(),
                    StandardWind.SOUTH to emptyList(),
                    StandardWind.WEST to emptyList()
                )
                val payments = calculator.computeExhaustiveDrawPayments(
                    playerWinds = playerWinds,
                    discards = discards,
                    openHands = openHands,
                    riichiSticks = 0,
                    honbaSticks = 0,
                    dealerWind = StandardWind.EAST
                )
                payments.values.sum() shouldBe 0
            }
        }
    }
})
