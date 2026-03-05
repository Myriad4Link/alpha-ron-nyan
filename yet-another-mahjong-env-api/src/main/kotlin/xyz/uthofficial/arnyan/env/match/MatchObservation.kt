package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.ReadOnlyPlayer
import xyz.uthofficial.arnyan.env.tile.ReadOnlyTileWall
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.RoundRotationStatus
import xyz.uthofficial.arnyan.env.wind.TableTopology
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.Yaku
import xyz.uthofficial.arnyan.env.yaku.YakuConfiguration
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.scoring.ScoringCalculator
import xyz.uthofficial.arnyan.env.scoring.LimitHand
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu

object EmptyYakuConfiguration : YakuConfiguration {
    override infix fun Int.han(block: () -> Unit) {
        block()
    }

    override fun evaluate(context: YakuContext, partitions: List<LongArray>): List<Pair<Yaku<LongArray>, Int>> {
        return emptyList()
    }
}

object EmptyScoringCalculator : ScoringCalculator {
    override fun calculateFu(
        hand: List<Tile>,
        mentsus: List<Mentsu>,
        context: YakuContext,
        winnerWind: Wind,
        isDealer: Boolean
    ): Int = 20

    override fun calculateBasicPoints(
        han: Int,
        fu: Int,
        isDealer: Boolean,
        isTsumo: Boolean
    ): Int = 0

    override fun determineLimitHand(han: Int): LimitHand? = null

    override fun distributePayments(
        winnerWind: Wind,
        basicPoints: Int,
        isTsumo: Boolean,
        isDealer: Boolean,
        riichiSticks: Int,
        honbaSticks: Int,
        playerWinds: List<Wind>,
        loserWind: Wind?
    ): Map<Wind, Int> = emptyMap()

    override fun computeExhaustiveDrawPayments(
        playerWinds: List<Wind>,
        discards: Map<Wind, List<Tile>>,
        openHands: Map<Wind, List<List<Tile>>>,
        riichiSticks: Int,
        honbaSticks: Int,
        dealerWind: Wind
    ): Map<Wind, Int> = emptyMap()
}

data class MatchObservation(
    val players: List<ReadOnlyPlayer>,
    val wall: ReadOnlyTileWall,
    val topology: TableTopology,
    val currentSeatWind: Wind,
    val roundRotationStatus: RoundRotationStatus,
    val discards: Map<Wind, List<Tile>> = emptyMap(),
    val lastAction: LastAction = LastAction.None,
    val availableActions: List<Action> = emptyList(),
    val yakuConfiguration: YakuConfiguration = EmptyYakuConfiguration,
    val scoringCalculator: ScoringCalculator = EmptyScoringCalculator,
    val riichiSticks: Int = 0,
    val honbaSticks: Int = 0
)
