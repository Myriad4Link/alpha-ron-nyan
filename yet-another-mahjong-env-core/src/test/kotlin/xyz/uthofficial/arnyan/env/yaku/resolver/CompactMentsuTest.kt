package xyz.uthofficial.arnyan.env.yaku.resolver

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.tile.*

class CompactMentsuTest : FunSpec({
    // Helper to get tile index for a given TileType and value
    fun tileIndex(tileType: TileType, value: Int): Int {
        return when (tileType) {
            Dragon -> value - 1 // Dragon values 1-3 map to indices 0-2
            Man -> value + 2    // Man values 1-9 map to indices 3-11
            Pin -> value + 11   // Pin values 1-9 map to indices 12-20
            Sou -> value + 20   // Sou values 1-9 map to indices 21-29
            Wind -> value + 29  // Wind values 1-4 map to indices 30-33
            else -> error("Unsupported tile type")
        }
    }

    // Helper to create tile indices array for a sequence
    fun tileIndices(vararg pairs: Pair<TileType, Int>): IntArray {
        return pairs.map { (type, value) -> tileIndex(type, value) }.toIntArray()
    }

    test("pack with 3 tiles works correctly") {
        val indices = tileIndices(Man to 1, Man to 2, Man to 3)
        val typeIndex = MentsuTypeRegistry.getIndex(Shuntsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex)

        val compact = CompactMentsu(packed)
        compact.tile1Index shouldBe tileIndex(Man, 1)
        compact.tile2Index shouldBe tileIndex(Man, 2)
        compact.tile3Index shouldBe tileIndex(Man, 3)
        compact.tile4Index shouldBe 0
        compact.mentsuType shouldBe Shuntsu
        compact.isOpen shouldBe false
        compact.akas shouldBe emptyList()
    }

    test("pack with 4 tiles works correctly") {
        val indices = tileIndices(Man to 1, Man to 1, Man to 1, Man to 1)
        val typeIndex = MentsuTypeRegistry.getIndex(Kantsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex)

        val compact = CompactMentsu(packed)
        compact.tile1Index shouldBe tileIndex(Man, 1)
        compact.tile2Index shouldBe tileIndex(Man, 1)
        compact.tile3Index shouldBe tileIndex(Man, 1)
        compact.tile4Index shouldBe tileIndex(Man, 1)
        compact.mentsuType shouldBe Kantsu
    }

    test("pack with isOpen flag sets flag correctly") {
        val indices = tileIndices(Sou to 5, Sou to 5, Sou to 5)
        val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex, isOpen = true)

        val compact = CompactMentsu(packed)
        compact.isOpen shouldBe true
    }

     test("pack with aka presence encodes correctly") {
          val indices = tileIndices(Pin to 3, Pin to 3, Pin to 3)
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          val akaPresence = 0b101
          val packed = CompactMentsu.pack(indices, 0, typeIndex, akaPresence = akaPresence)
 
         val compact = CompactMentsu(packed)
         compact.akas.size shouldBe 2
         compact.akas[0].value shouldBe 3
         compact.akas[0].tileType shouldBe Pin
         compact.akas[0].isAka shouldBe true
         compact.akas[1].value shouldBe 3
         compact.akas[1].tileType shouldBe Pin
         compact.akas[1].isAka shouldBe true

         val tiles = compact.tiles
         tiles.size shouldBe 3
         tiles[0].isAka shouldBe true
         tiles[1].isAka shouldBe false
         tiles[2].isAka shouldBe true
         compact.akas shouldBe listOf(tiles[0], tiles[2])
     }

     test("pack masks tile index to 8 bits") {
          val indices = intArrayOf(300)
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          val packed = CompactMentsu.pack(indices, 0, typeIndex)
         val compact = CompactMentsu(packed)
         compact.tile1Index shouldBe 44
     }

     test("pack masks mentsu type index to 8 bits") {
          val indices = tileIndices(Man to 1, Man to 1, Man to 1)
          val typeIndex = 300
          val packed = CompactMentsu.pack(indices, 0, typeIndex)
         val compact = CompactMentsu(packed)
         compact.tile1Index shouldBe tileIndex(Man, 1)
         compact.tile2Index shouldBe tileIndex(Man, 1)
         compact.tile3Index shouldBe tileIndex(Man, 1)
     }

     test("pack masks aka presence bits") {
          val indices = tileIndices(Man to 1, Man to 1, Man to 1)
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          val akaPresence = 200
          val packed = CompactMentsu.pack(indices, 0, typeIndex, akaPresence = akaPresence)
         val compact = CompactMentsu(packed)
         compact.akas shouldBe emptyList()
     }

     test("tiles property returns correct Tile objects") {
          val indices = tileIndices(Wind to 1, Wind to 1, Wind to 1)
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          val packed = CompactMentsu.pack(indices, 0, typeIndex)
 
         val compact = CompactMentsu(packed)
         val tiles = compact.tiles
         tiles.size shouldBe 3
         tiles[0] shouldBe Tile(Wind, 1, false)
         tiles[1] shouldBe Tile(Wind, 1, false)
         tiles[2] shouldBe Tile(Wind, 1, false)
         tiles.forEach { it.isAka shouldBe false }
     }

    test("tiles filters out zero indices") {
        val indices = intArrayOf(tileIndex(Man, 1), tileIndex(Man, 2))
        val typeIndex = MentsuTypeRegistry.getIndex(Shuntsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex)

        val compact = CompactMentsu(packed)
        compact.tiles.size shouldBe 2
        compact.tiles[0] shouldBe Tile(Man, 1, false)
        compact.tiles[1] shouldBe Tile(Man, 2, false)
    }

     test("pack with maximum tile indices works") {
          val indices = intArrayOf(33, 32, 31)
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          val packed = CompactMentsu.pack(indices, 0, typeIndex, isOpen = true, akaPresence = 7)
 
         val compact = CompactMentsu(packed)
         compact.tile1Index shouldBe 33
         compact.tile2Index shouldBe 32
         compact.tile3Index shouldBe 31
         compact.tile4Index shouldBe 0
         compact.isOpen shouldBe true

         compact.akas.size shouldBe 3
         compact.akas shouldBe compact.tiles
         compact.tiles.forEach { it.isAka shouldBe true }
     }

     test("pack throws IllegalStateException for tile indices size > 4") {
          val indices = intArrayOf(1, 2, 3, 4, 5) // size 5
          val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
          shouldThrow<IllegalStateException> {
              CompactMentsu.pack(indices, 0, typeIndex)
         }
     }

    test("pack with empty tile indices works") {
        val indices = intArrayOf()
        val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex)
        val compact = CompactMentsu(packed)
        compact.tiles shouldBe emptyList()
        compact.tile1Index shouldBe 0
        compact.tile2Index shouldBe 0
        compact.tile3Index shouldBe 0
        compact.tile4Index shouldBe 0
    }

    test("value class equality works") {
        val indices = tileIndices(Dragon to 1, Dragon to 1, Dragon to 1)
        val typeIndex = MentsuTypeRegistry.getIndex(Koutsu)
        val packed = CompactMentsu.pack(indices, 0, typeIndex)
        val compact1 = CompactMentsu(packed)
        val compact2 = CompactMentsu(packed)

        (compact1 == compact2) shouldBe true
        (compact1.hashCode() == compact2.hashCode()) shouldBe true
    }
})