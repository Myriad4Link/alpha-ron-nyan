package xyz.uthofficial.arnyan.env.yaku

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

class DoraCalculatorTest : FunSpec({
    
    test("countDora should count matching tiles") {
        val hand = listOf(
            Tile(Man, 5, false),
            Tile(Man, 6, false),
            Tile(Pin, 3, false)
        )
        val doraIndicators = listOf(Tile(Man, 4, false))
        
        val count = DoraCalculator.countDora(hand, doraIndicators)
        count shouldBe 1
    }
    
    test("countDora should count multiple matching tiles") {
        val hand = listOf(
            Tile(Man, 5, false),
            Tile(Man, 5, false),
            Tile(Man, 5, false)
        )
        val doraIndicators = listOf(Tile(Man, 4, false))
        
        val count = DoraCalculator.countDora(hand, doraIndicators)
        count shouldBe 3
    }
    
    test("countDora should count aka dora") {
        val hand = listOf(
            Tile(Pin, 5, true),
            Tile(Sou, 5, true)
        )
        val doraIndicators = emptyList<Tile>()
        
        val count = DoraCalculator.countDora(hand, doraIndicators, includeAka = true)
        count shouldBe 2
    }
    
    test("countDora should count both regular and aka dora") {
        val hand = listOf(
            Tile(Man, 5, false),
            Tile(Pin, 5, true)
        )
        val doraIndicators = listOf(Tile(Man, 4, false))
        
        val count = DoraCalculator.countDora(hand, doraIndicators, includeAka = true)
        count shouldBe 2
    }
    
    test("getNextDora should work for manzu") {
        DoraCalculator.getNextDora(Tile(Man, 1, false)).value shouldBe 2
        DoraCalculator.getNextDora(Tile(Man, 8, false)).value shouldBe 9
        DoraCalculator.getNextDora(Tile(Man, 9, false)).value shouldBe 1
    }
    
    test("getNextDora should work for pinzu") {
        DoraCalculator.getNextDora(Tile(Pin, 1, false)).value shouldBe 2
        DoraCalculator.getNextDora(Tile(Pin, 8, false)).value shouldBe 9
        DoraCalculator.getNextDora(Tile(Pin, 9, false)).value shouldBe 1
    }
    
    test("getNextDora should work for souzu") {
        DoraCalculator.getNextDora(Tile(Sou, 1, false)).value shouldBe 2
        DoraCalculator.getNextDora(Tile(Sou, 8, false)).value shouldBe 9
        DoraCalculator.getNextDora(Tile(Sou, 9, false)).value shouldBe 1
    }
    
    test("getNextDora should work for wind tiles") {
        DoraCalculator.getNextDora(Tile(Wind, 1, false)).value shouldBe 2
        DoraCalculator.getNextDora(Tile(Wind, 2, false)).value shouldBe 3
        DoraCalculator.getNextDora(Tile(Wind, 3, false)).value shouldBe 4
        DoraCalculator.getNextDora(Tile(Wind, 4, false)).value shouldBe 1
    }
    
    test("getNextDora should work for dragon tiles") {
        DoraCalculator.getNextDora(Tile(Dragon, 1, false)).value shouldBe 2
        DoraCalculator.getNextDora(Tile(Dragon, 2, false)).value shouldBe 3
        DoraCalculator.getNextDora(Tile(Dragon, 3, false)).value shouldBe 1
    }
    
    test("getDoraTiles should return correct tiles") {
        val indicators = listOf(
            Tile(Man, 5, false),
            Tile(Pin, 9, false),
            Tile(Wind, 4, false),
            Tile(Dragon, 3, false)
        )
        
        val doraTiles = DoraCalculator.getDoraTiles(indicators)
        
        doraTiles.size shouldBe 4
        doraTiles[0].tileType shouldBe Man
        doraTiles[0].value shouldBe 6
        doraTiles[1].tileType shouldBe Pin
        doraTiles[1].value shouldBe 1
        doraTiles[2].tileType shouldBe Wind
        doraTiles[2].value shouldBe 1
        doraTiles[3].tileType shouldBe Dragon
        doraTiles[3].value shouldBe 1
    }
})
