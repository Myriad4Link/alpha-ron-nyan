package xyz.uthofficial.arnyan.env.yaku

import xyz.uthofficial.arnyan.env.tile.Dragon
import xyz.uthofficial.arnyan.env.tile.Man
import xyz.uthofficial.arnyan.env.tile.Pin
import xyz.uthofficial.arnyan.env.tile.Sou
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileType
import xyz.uthofficial.arnyan.env.tile.Wind
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

object DoraCalculator {
    
    fun countDora(
        hand: List<Tile>,
        doraIndicators: List<Tile>,
        includeAka: Boolean = true
    ): Int {
        val doraTiles = getDoraTiles(doraIndicators)
        var count = 0
        
        for (tile in hand) {
            if (isDoraTile(tile, doraTiles)) {
                count++
            }
            if (includeAka && tile.isAka) {
                count++
            }
        }
        
        return count
    }
    
    private fun isDoraTile(tile: Tile, doraTiles: List<Tile>): Boolean {
        return doraTiles.any { dora ->
            dora.tileType == tile.tileType && dora.value == tile.value
        }
    }
    
    fun getDoraTiles(indicators: List<Tile>): List<Tile> {
        return indicators.map { indicator ->
            getNextDora(indicator)
        }
    }
    
    fun getNextDora(indicator: Tile): Tile {
        val nextValue = when (indicator.tileType) {
            Man -> {
                if (indicator.value == 9) 1 else indicator.value + 1
            }
            Pin -> {
                if (indicator.value == 9) 1 else indicator.value + 1
            }
            Sou -> {
                if (indicator.value == 9) 1 else indicator.value + 1
            }
            Wind -> {
                if (indicator.value == 4) 1 else indicator.value + 1
            }
            Dragon -> {
                when (indicator.value) {
                    1 -> 2
                    2 -> 3
                    3 -> 1
                    else -> indicator.value
                }
            }
            else -> indicator.value
        }
        
        return Tile(indicator.tileType, nextValue, false)
    }
    
    fun Tile.index(): Int {
        val segment = TileTypeRegistry.getSegment(tileType)
        return segment[0] + value - 1
    }
}
