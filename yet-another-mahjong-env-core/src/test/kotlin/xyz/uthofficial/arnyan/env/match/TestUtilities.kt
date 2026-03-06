package xyz.uthofficial.arnyan.env.match

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.match.TestTileFactory.createDeterministicNonWinningHand
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.ruleset.RuleSet
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.wind.CyclicTableTopology
import xyz.uthofficial.arnyan.env.wind.StandardRoundWindCycle
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.StandardYakuRule
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import java.util.*
import xyz.uthofficial.arnyan.env.wind.Wind as SeatWind

// Reusable test player following existing pattern
data class DummyPlayer(
    override val id: UUID = UUID.randomUUID(),
    override val closeHand: MutableList<Tile> = mutableListOf(),
    override val openHand: MutableList<List<Tile>> = mutableListOf(),
    override val currentMentsusComposition: MutableList<List<Mentsu>> = mutableListOf(),
    override var seat: SeatWind? = null,
    override var score: Int = 0,
    override var isRiichiDeclared: Boolean = false,
    override var riichiSticksDeposited: Int = 0,
    override var nukiCount: Int = 0
) : Player

// Constants for simplified test wall
const val SIMPLIFIED_WALL_TILE_TYPES = 5 // Man1, Man2, Man3, Pin1, Pin2
const val SIMPLIFIED_WALL_TILES_PER_TYPE = 8
const val SIMPLIFIED_WALL_TOTAL_TILES = SIMPLIFIED_WALL_TILE_TYPES * SIMPLIFIED_WALL_TILES_PER_TYPE
const val STANDARD_DEAL_AMOUNT = 13

// Tile creation utilities
object TestTileFactory {
    fun createMan(value: Int, isAka: Boolean = false): Tile = Tile(Man, value, isAka)
    fun createPin(value: Int, isAka: Boolean = false): Tile = Tile(Pin, value, isAka)
    fun createSou(value: Int, isAka: Boolean = false): Tile = Tile(Sou, value, isAka)
    fun createWind(value: Int, isAka: Boolean = false): Tile = Tile(Wind, value, isAka)
    fun createDragon(value: Int, isAka: Boolean = false): Tile = Tile(Dragon, value, isAka)
    fun createNorthWind(): Tile = Tile(Wind, 4, false)

    // Create a simple hand of identical tiles for testing
    fun createHandOfSameTile(tileType: TileType, value: Int, count: Int): List<Tile> {
        return List(count) { Tile(tileType, value, false) }
    }

    // Create simplified wall for faster tests (just Man 1-3, Pin 1-2)
    fun create40Wall(): List<Tile> {
        val tiles = mutableListOf<Tile>()
        repeat(SIMPLIFIED_WALL_TILES_PER_TYPE) { tiles.add(Tile(Man, 1, false)) }
        repeat(SIMPLIFIED_WALL_TILES_PER_TYPE) { tiles.add(Tile(Man, 2, false)) }
        repeat(SIMPLIFIED_WALL_TILES_PER_TYPE) { tiles.add(Tile(Man, 3, false)) }
        repeat(SIMPLIFIED_WALL_TILES_PER_TYPE) { tiles.add(Tile(Pin, 1, false)) }
        repeat(SIMPLIFIED_WALL_TILES_PER_TYPE) { tiles.add(Tile(Pin, 2, false)) }
        return tiles.shuffled()
    }

    // Create a wall with at most 2 copies per tile type to avoid pon/kan intercepts
    fun createUniqueWall(): List<Tile> {
        val tiles = mutableListOf<Tile>()
        // 1 copy of each tile type
        for (value in 1..9) tiles.add(Tile(Man, value, false))
        for (value in 1..9) tiles.add(Tile(Pin, value, false))
        for (value in 1..9) tiles.add(Tile(Sou, value, false))
        for (value in 1..4) tiles.add(Tile(Wind, value, false))
        for (value in 1..3) tiles.add(Tile(Dragon, value, false))
        // extra copies (6) to reach 40 tiles, using Man1..Man6
        for (value in 1..6) tiles.add(Tile(Man, value, false))
        return tiles.shuffled()
    }

    /**
     * Assign deterministic hands to players ensuring no intercept actions are possible.
     * Each player gets tiles from a different suit, and within each suit, values are spaced
     * to prevent chii sequences.
     * 
     * @param players List of players in seat order (EAST, SOUTH, WEST, NORTH)
     * @param dealAmount Number of tiles each player should have
     * @return A safe tile that EAST player can discard without triggering intercepts
     */
    fun assignDeterministicHands(players: List<DummyPlayer>, dealAmount: Int): Tile {
        require(players.size in 2..4) { "Only 2-4 players supported" }

        // Clear all hands
        players.forEach { it.closeHand.clear() }

        // Assign suits to players based on position
        val suits = listOf(Man, Pin, Sou, Wind).take(players.size)

        // For each player, give them tiles from their assigned suit
        // Use values that are spaced to prevent chii sequences
        // For number suits (Man, Pin, Sou): use values 1, 4, 7, 2, 5, 8, 3, 6, 9 repeating
        // For Wind: use values 1, 2, 3, 4 repeating
        // For Dragon: use values 1, 2, 3 repeating (if needed)

        players.forEachIndexed { index, player ->
            val suit = suits[index]
            val values = when (suit) {
                is Man, is Pin, is Sou -> {
                    // Spaced values to prevent chii: 1,4,7,2,5,8,3,6,9 pattern
                    generateSequence(1) { it + 3 }
                        .take(9)
                        .flatMap { listOf(it, it + 3, it + 6) }
                        .filter { it in 1..9 }
                        .distinct()
                        .toList()
                }

                is Wind -> {
                    // Wind values 1-4 repeating
                    generateSequence(1) { (it % 4) + 1 }.take(dealAmount).toList()
                }

                is Dragon -> {
                    // Dragon values 1-3 repeating
                    generateSequence(1) { (it % 3) + 1 }.take(dealAmount).toList()
                }

                else -> error("Unsupported tile type")
            }

            // Add tiles to player's hand
            repeat(dealAmount) { i ->
                val value = values[i % values.size]
                player.closeHand.add(Tile(suit, value, false))
            }
        }

        // Return a safe discard tile from EAST player
        // EAST is first player (index 0)
        val eastPlayer = players[0]
        return eastPlayer.closeHand.first()
    }

    /**
     * Create a deterministic hand of tiles from a single suit that cannot form winning combinations.
     * Uses spaced values (1,4,7,2,5,8,3,6,9 pattern) to prevent chii sequences and ensures
     * no more than 2 copies of any value to prevent pon/kantsu.
     * 
     * @param suit The tile suit to use (Man, Pin, or Sou)
     * @param count Number of tiles to generate
     * @return List of tiles forming a non-winning hand
     */
    fun createDeterministicNonWinningHand(suit: TileType, count: Int): List<Tile> {
        require(suit is Man || suit is Pin || suit is Sou) { "Only number suits (Man, Pin, Sou) supported" }

        // Spaced values to prevent chii sequences: 1,4,7,2,5,8,3,6,9 pattern
        val spacedValues = listOf(1, 4, 7, 2, 5, 8, 3, 6, 9)

        return List(count) { i ->
            val value = spacedValues[i % spacedValues.size]
            Tile(suit, value, false)
        }
    }

    /**
     * Fill a player's hand with a deterministic non-winning pattern.
     * Convenience method that clears the player's hand and calls [createDeterministicNonWinningHand].
     * 
     * @param player The player whose hand to fill
     * @param suit The tile suit to use (Man, Pin, or Sou)
     * @param count Number of tiles to add
     */
    fun fillWithDeterministicNonWinningHand(player: DummyPlayer, suit: TileType, count: Int) {
        player.closeHand.clear()
        player.closeHand.addAll(createDeterministicNonWinningHand(suit, count))
    }
}

// Tile printing utilities
fun Tile.toHumanString(): String {
    return when (tileType) {
        is Man -> "${value}m"
        is Pin -> "${value}p"
        is Sou -> "${value}s"
        is Wind -> when (value) {
            1 -> "E"
            2 -> "S"
            3 -> "W"
            4 -> "N"
            else -> "Wind$value"
        }

        is Dragon -> when (value) {
            1 -> "White"
            2 -> "Green"
            3 -> "Red"
            else -> "Dragon$value"
        }

        else -> "$tileType$value"
    } + if (isAka) " (aka)" else ""
}

fun List<Tile>.toHumanString(): String = this.sortedWith(compareBy({ it.tileType::class.simpleName }, { it.value }))
    .joinToString(", ") { it.toHumanString() }

// Player lookup utility
fun List<DummyPlayer>.getPlayerSitAt(seat: xyz.uthofficial.arnyan.env.wind.Wind): DummyPlayer =
    this.find { it.seat == seat } ?: error("No player found at seat $seat")

fun getPlayerBySeat(players: List<DummyPlayer>, seat: StandardWind): DummyPlayer =
    players.find { it.seat == seat } ?: error("No player found at seat $seat")

// Create a simple yaku configuration for testing with basic yaku rules
fun createSimpleYakuConfiguration(): xyz.uthofficial.arnyan.env.yaku.YakuConfiguration {
    return xyz.uthofficial.arnyan.env.yaku.StandardYakuRule.build()
}

// Create a simple rule set for testing using existing DSL
fun createSimpleRuleSet(tiles: List<Tile> = TestTileFactory.create40Wall()): RuleSet {
    return RuleSet(
        wallGenerationRule = {
            // Create a tile wall with standard deal amount and add tiles
            val wall = StandardTileWall(standardDealAmount = STANDARD_DEAL_AMOUNT)
            wall.addAll(tiles)
            Result.Success(wall)
        },
        playerWindRotationOrderRule = {
            // Simple 3-player topology (EAST, SOUTH, WEST)
            val topology = CyclicTableTopology(
                seats = listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST)
            )
            Result.Success(topology)
        },
        roundWindRotationRule = {
            // Simple round wind cycle: just EAST 1 round
            val cycle = StandardRoundWindCycle.fromMap(mapOf(StandardWind.EAST to 1)).getOrThrow()
            Result.Success(cycle)
        },
        yakuRule = StandardYakuRule
    )
}

// Create a 4-player rule set for testing
fun createFourPlayerRuleSet(tiles: List<Tile> = TestTileFactory.create40Wall() + TestTileFactory.create40Wall()): RuleSet {
    return RuleSet(
        wallGenerationRule = {
            val wall = StandardTileWall(standardDealAmount = STANDARD_DEAL_AMOUNT)
            wall.addAll(tiles)
            Result.Success(wall)
        },
        playerWindRotationOrderRule = {
            val topology = CyclicTableTopology(
                seats = listOf(StandardWind.EAST, StandardWind.SOUTH, StandardWind.WEST, StandardWind.NORTH)
            )
            Result.Success(topology)
        },
        roundWindRotationRule = {
            val cycle = StandardRoundWindCycle.fromMap(mapOf(StandardWind.EAST to 1)).getOrThrow()
            Result.Success(cycle)
        },
        yakuRule = StandardYakuRule
    )
}

// Match builder for fluent test setup
class MatchBuilder {
    private var players: List<DummyPlayer> = emptyList()
    private var ruleSet: RuleSet? = null
    private var shuffleWinds: Boolean = false
    private var listeners: List<MatchListener> = emptyList()
    private var wallTiles: List<Tile>? = null

    fun withPlayers(count: Int): MatchBuilder {
        players = List(count) { DummyPlayer() }
        return this
    }

    fun withCustomPlayers(vararg players: DummyPlayer): MatchBuilder {
        this.players = players.toList()
        return this
    }

    fun withRuleSet(ruleSet: RuleSet): MatchBuilder {
        this.ruleSet = ruleSet
        return this
    }

    fun withShuffleWinds(shuffle: Boolean): MatchBuilder {
        shuffleWinds = shuffle
        return this
    }

    fun withListeners(listeners: List<MatchListener>): MatchBuilder {
        this.listeners = listeners
        return this
    }

    /**
     * Specify the exact tiles to use for the wall. Makes wall size explicit in tests.
     * If not called, defaults to [TestTileFactory.create40Wall].
     */
    fun withWallTiles(tiles: List<Tile>): MatchBuilder {
        this.wallTiles = tiles
        return this
    }

    fun build(): Match {
        val finalRuleSet = ruleSet ?: createSimpleRuleSet(wallTiles ?: TestTileFactory.create40Wall())

        return Match.create(finalRuleSet, listeners, players, shuffleWinds)
            .getOrThrow()
    }
}

// Result assertion extensions
fun <T> Result<T, ActionError>.shouldBeSuccess(): T {
    this.shouldBeInstanceOf<Result.Success<T>>()
    return (this as Result.Success).value
}

fun <T> Result<T, ActionError>.shouldBeFailureWithError(error: MatchError) {
    this.shouldBeInstanceOf<Result.Failure<ActionError>>()
    val actionError = (this as Result.Failure).error
    actionError.shouldBeInstanceOf<ActionError.Match>()
    (actionError as ActionError.Match).error shouldBe error
}

fun <T> Result<T, ActionError>.shouldBeFailureWithPlayerNotInMatch() {
    this.shouldBeInstanceOf<Result.Failure<ActionError>>()
    val actionError = (this as Result.Failure).error
    actionError.shouldBeInstanceOf<ActionError.Match>()
    actionError.error.shouldBeInstanceOf<MatchError.PlayerNotInMatch>()
}

fun <T> Result<T, ActionError>.shouldBeFailureWithActionNotAvailable() {
    this.shouldBeInstanceOf<Result.Failure<ActionError>>()
    val actionError = (this as Result.Failure).error
    actionError.shouldBeInstanceOf<ActionError.Match>()
    (actionError as ActionError.Match).error.shouldBeInstanceOf<MatchError.ActionNotAvailable>()
}

fun <T> Result<T, ActionError>.shouldBeFailureWithNotPlayersTurn() {
    this.shouldBeInstanceOf<Result.Failure<ActionError>>()
    val actionError = (this as Result.Failure).error
    actionError.shouldBeInstanceOf<ActionError.Match>()
    (actionError as ActionError.Match).error.shouldBeInstanceOf<MatchError.NotPlayersTurn>()
}

// Extension to convert MatchObservation to MatchState for testing
internal fun MatchObservation.toState(): MatchState = MatchState(
    players = players.map { it as Player },
    wall = wall as TileWall,
    topology = topology,
    currentSeatWind = currentSeatWind,
    roundRotationStatus = roundRotationStatus,
    discards = discards.mapValues { (_, list) -> list.toMutableList() }.toMutableMap(),
    lastAction = lastAction,
    yakuConfiguration = yakuConfiguration,
    scoringCalculator = scoringCalculator,
    furitenPlayers = furitenPlayers.toMutableSet(),
    temporaryFuritenPlayers = temporaryFuritenPlayers.toMutableSet(),
    riichiSticks = riichiSticks,
    honbaSticks = honbaSticks,
    doraIndicators = doraIndicators.toMutableList()
)

// Extension to convert MatchState to MatchObservation for testing
internal fun MatchState.toObservation(): MatchObservation = MatchObservation(
    players = players,
    wall = wall,
    topology = topology,
    currentSeatWind = currentSeatWind,
    roundRotationStatus = roundRotationStatus,
    discards = discards.mapValues { (_, list) -> list.toList() },
    lastAction = lastAction,
    yakuConfiguration = yakuConfiguration,
    scoringCalculator = scoringCalculator,
    riichiSticks = riichiSticks,
    honbaSticks = honbaSticks,
    furitenPlayers = furitenPlayers.toSet(),
    temporaryFuritenPlayers = temporaryFuritenPlayers.toSet(),
    doraIndicators = doraIndicators.toList()
)