package xyz.uthofficial.arnyan.env.scoring

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu

/**
 * Calculates fu, points, and payment distribution for a winning hand.
 */
interface ScoringCalculator {
    /**
     * Calculates the fu value of a winning hand.
     * @param hand The complete hand (including winning tile) as a list of tiles.
     * @param mentsus The resolved mentsus (including pair) of the hand.
     * @param context Yaku context (winning method, open/closed, riichi, etc.)
     * @param winnerWind The wind (seat) of the winning player.
     * @param isDealer Whether the winner is the dealer.
     * @return The total fu value (rounded up to nearest 10).
     */
    fun calculateFu(
        hand: List<Tile>,
        mentsus: List<Mentsu>,
        context: YakuContext,
        winnerWind: Wind,
        isDealer: Boolean
    ): Int

    /**
     * Calculates the basic points from han and fu.
     * @param han Total han count (including dora).
     * @param fu Total fu value.
     * @param isDealer Whether the winner is the dealer.
     * @param isTsumo Whether the win is by self-draw.
     * @return Basic points (before honba and riichi sticks).
     */
    fun calculateBasicPoints(
        han: Int,
        fu: Int,
        isDealer: Boolean,
        isTsumo: Boolean
    ): Int

    /**
     * Determines if the hand is a limit hand (mangan, haneman, etc.)
     * @param han Total han count.
     * @return The corresponding limit hand, or null if not a limit hand.
     */
    fun determineLimitHand(han: Int): LimitHand?

    /**
     * Distributes payments after a win.
     * @param winnerWind The wind (seat) of the winning player.
     * @param basicPoints The basic points calculated.
     * @param isTsumo Whether the win is by self-draw.
     * @param isDealer Whether the winner is the dealer.
     * @param riichiSticks Number of riichi sticks on the table.
     * @param honbaSticks Number of honba sticks (100-point sticks) on the table.
     * @param playerWinds List of all player winds in seating order.
     * @param loserWind The wind of the player who discarded the winning tile (for Ron only); null for Tsumo.
     * @return Map from each player's wind to the amount they pay (negative) or receive (positive).
     *         The sum of payments should be zero.
     */
    fun distributePayments(
        winnerWind: Wind,
        basicPoints: Int,
        isTsumo: Boolean,
        isDealer: Boolean,
        riichiSticks: Int,
        honbaSticks: Int,
        playerWinds: List<Wind>,
        loserWind: Wind? = null
    ): Map<Wind, Int>

    /**
     * Computes payments for exhaustive draw (ryuukyoku) including nagashi mangan.
     * @param playerWinds List of all player winds in seating order.
     * @param discards Map from player wind to their unclaimed discards.
     * @param openHands Map from player wind to their open hand (calls). Each call is a list of tiles. Empty list means no calls.
     * @param riichiSticks Number of riichi sticks on the table.
     * @param honbaSticks Number of honba sticks (100-point sticks) on the table.
     * @param dealerWind The wind of the dealer (East).
     * @return Map from each player's wind to the amount they pay (negative) or receive (positive).
     *         The sum of payments should be zero.
     */
    fun computeExhaustiveDrawPayments(
        playerWinds: List<Wind>,
        discards: Map<Wind, List<Tile>>,
        openHands: Map<Wind, List<List<Tile>>>,
        riichiSticks: Int,
        honbaSticks: Int,
        dealerWind: Wind
    ): Map<Wind, Int>
}

enum class LimitHand {
    MANGAN,
    HANEMAN,
    BAIMAN,
    SANBAIMAN,
    YAKUMAN
}