package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding

/**
 * Standard implementation of TileWall.
 * 
 * Dead Wall Structure:
 * - 14 tiles arranged as 7 stacks of 2 tiles each (upper + lower)
 * - Initial dora indicator: upper tile on 3rd stack from dead wall end
 * - Kan: reveals upper tile on next stack sequentially
 * - Riichi: reveals lower tile on next stack sequentially
 * 
 * Stack layout (indices in flat deadWallTiles list):
 * Stack 0: indices 0 (upper), 1 (lower)
 * Stack 1: indices 2 (upper), 3 (lower)
 * Stack 2: indices 4 (upper), 5 (lower) <- Initial dora (upper tile at index 4)
 * Stack 3: indices 6 (upper), 7 (lower) <- 1st kan dora (upper tile at index 6)
 * Stack 4: indices 8 (upper), 9 (lower) <- 2nd kan dora (upper tile at index 8)
 * Stack 5: indices 10 (upper), 11 (lower) <- 3rd kan dora (upper tile at index 10)
 * Stack 6: indices 12 (upper), 13 (lower) <- 4th kan dora (upper tile at index 12)
 */
class StandardTileWall(override val standardDealAmount: Int) : TileWall {
    private val tiles = ArrayDeque<Tile>()
    private var deadWallTiles: List<Tile> = emptyList()
    
    /**
     * Current dora indicator stack index (0-6 for 7 stacks).
     * Starts at 2 (3rd stack) for initial dora.
     */
    private var doraStackIndex: Int = 0
    
    /**
     * Whether the lower tile of the current stack has been revealed.
     * - false: only upper tile revealed (normal state after kan)
     * - true: both upper and lower tiles revealed (after riichi)
     */
    private var lowerTileRevealed: Boolean = false
    
    override val tileWall: List<Tile>
        get() = tiles + deadWallTiles

    override val size
        get() = tiles.size

    override val doraIndicators: List<Tile>
        get() {
            if (doraStackIndex >= DEAD_WALL_STACK_COUNT) return emptyList()
            
            val upperTileIndex = doraStackIndex * 2
            if (upperTileIndex >= deadWallTiles.size) return emptyList()
            
            val indicators = mutableListOf(deadWallTiles[upperTileIndex])
            
            // Include lower tile if it has been revealed (e.g., after riichi)
            if (lowerTileRevealed) {
                val lowerTileIndex = upperTileIndex + 1
                if (lowerTileIndex < deadWallTiles.size) {
                    indicators.add(deadWallTiles[lowerTileIndex])
                }
            }
            
            return indicators
        }

    override val deadWallRemaining: Int
        get() {
            // Count revealed tiles: 1 per stack (upper) + 1 if lower revealed
            val revealedCount = (doraStackIndex - INITIAL_DORA_STACK_INDEX + 1) + (if (lowerTileRevealed) 1 else 0)
            return deadWallTiles.size - revealedCount
        }

    override fun add(tile: Tile) = tiles.add(tile)
    override fun addAll(tiles: Collection<Tile>) = this@StandardTileWall.tiles.addAll(tiles)

    override fun shuffle() {
        tiles.shuffle()
        deadWallTiles = emptyList()
        doraStackIndex = 0
        lowerTileRevealed = false
    }

    override fun draw(amount: Int): Result<List<Tile>, WallError> {
        if (tiles.size < amount) return Result.Failure(WallError.NotEnoughTiles(amount, tiles.size))
        return Result.Success(List(amount) { tiles.removeLast() })
    }

    override fun initializeDeadWall(deadWallSize: Int) {
        if (tiles.size < deadWallSize) return
        
        // Take tiles from end and reverse to maintain proper order
        deadWallTiles = tiles.takeLast(deadWallSize).reversed()
        repeat(deadWallSize) { tiles.removeLast() }
        
        // Start at stack 2 (3rd stack) for initial dora indicator
        doraStackIndex = INITIAL_DORA_STACK_INDEX
        lowerTileRevealed = false
    }

    /**
     * Reveal next dora indicator for KAN.
     * Reveals the UPPER tile on the next stack.
     */
    override fun revealNextDoraIndicator(): Result<Tile, WallError> {
        val nextStackIndex = doraStackIndex + 1
        
        if (nextStackIndex >= DEAD_WALL_STACK_COUNT) {
            return Result.Failure(WallError.NotEnoughTiles(1, 0))
        }
        
        val upperTileIndex = nextStackIndex * 2
        if (upperTileIndex >= deadWallTiles.size) {
            return Result.Failure(WallError.NotEnoughTiles(1, deadWallTiles.size - upperTileIndex))
        }
        
        doraStackIndex = nextStackIndex
        lowerTileRevealed = false
        return Result.Success(deadWallTiles[upperTileIndex])
    }
    
    /**
     * Reveal dora indicator for RIICHI.
     * Reveals the LOWER tile on the next stack (or current stack if not yet advanced).
     * 
     * @return Result with the revealed lower tile, or Failure if no tiles remain
     */
    override fun revealRiichiDoraIndicator(): Result<Tile, WallError> {
        // Determine which stack to reveal from
        val targetStackIndex = if (lowerTileRevealed) {
            // Already revealed lower tile, move to next stack
            doraStackIndex + 1
        } else {
            // Reveal lower tile of current stack
            doraStackIndex
        }
        
        if (targetStackIndex >= DEAD_WALL_STACK_COUNT) {
            return Result.Failure(WallError.NotEnoughTiles(1, 0))
        }
        
        val lowerTileIndex = targetStackIndex * 2 + 1
        if (lowerTileIndex >= deadWallTiles.size) {
            return Result.Failure(WallError.NotEnoughTiles(1, deadWallTiles.size - lowerTileIndex))
        }
        
        if (targetStackIndex > doraStackIndex) {
            doraStackIndex = targetStackIndex
        }
        lowerTileRevealed = true
        return Result.Success(deadWallTiles[lowerTileIndex])
    }

    override infix fun deal(amount: Int): TileWall.Dealer = StandardDealer(amount, this)

    class StandardDealer(private val amount: Int, private val wall: StandardTileWall) : TileWall.Dealer {
        override infix fun randomlyTo(players: List<Player>): Result<Unit, WallError> = binding {
            wall.shuffle()
            
            if (wall.size >= 100) {
                wall.initializeDeadWall()
            }

            players.forEach {
                val drawn = wall.draw(amount).bind()
                it.closeHand.addAll(drawn)
            }
        }
    }
    
    companion object {
        private const val DEAD_WALL_STACK_COUNT = 7
        private const val INITIAL_DORA_STACK_INDEX = 2 // 3rd stack (0-indexed)
    }
}
