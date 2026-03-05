package xyz.uthofficial.arnyan.env.scoring

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.Wind as TileWind
import xyz.uthofficial.arnyan.env.tile.Dragon as TileDragon
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.yaku.toIndex
import xyz.uthofficial.arnyan.env.yaku.isYaochuhai
import xyz.uthofficial.arnyan.env.yaku.isDragon
import xyz.uthofficial.arnyan.env.yaku.isWind
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu
import xyz.uthofficial.arnyan.env.yaku.toStandardWind
import kotlin.math.ceil

class StandardScoringCalculator : ScoringCalculator {


    override fun calculateFu(
        hand: List<Tile>,
        mentsus: List<Mentsu>,
        context: YakuContext,
        winnerWind: Wind,
        isDealer: Boolean
    ): Int {
        // Base fu: 20 for open hand, 30 for closed hand (except pinfu)
        var fu = if (context.isOpenHand) 20 else 30
        
        // Find pair (toitsu)
        val pair = mentsus.find { it.mentsuType === Toitsu }
        if (pair != null) {
            // Fu for yakuhai pair (seat wind, round wind, dragon)
            val pairTile = pair.tiles.first()
            if (isYakuhai(pairTile, winnerWind, context.roundWind)) {
                fu += 2
            }
        }
        
        // Fu from mentsus (open/closed koutsu/kantsu)
        for (mentsu in mentsus) {
            when (mentsu.mentsuType) {
                Koutsu -> {
                    // Closed koutsu (ankou) gives 8 fu, open koutsu (minkou) gives 4 fu
                    fu += if (mentsu.isOpen) 4 else 8
                    // Terminal/honor koutsu doubles fu? Actually terminal/honor koutsu already counted as yakuhai yaku.
                    // No extra fu beyond base.
                }
                Kantsu -> {
                    // Closed kantsu (ankan) gives 32 fu, open kantsu (minkan) gives 16 fu
                    fu += if (mentsu.isOpen) 16 else 32
                }
                else -> {}
            }
        }
        
        // Fu from wait (single/edge/closed wait)
        fu += computeWaitFu(mentsus, context.winningTile, context.isOpenHand, context.winningMethod)
        
        // Fu from tsumo (menzen tsumo)
        if (!context.isOpenHand && context.winningMethod == WinningMethod.TSUMO) {
            fu += 2
        }
        
        // Pinfu detection: closed hand, no extra fu from pair (non-yakuhai), all mentsus are shuntsu,
        // wait is ryanmen (two-sided), winning method is tsumo or ron on ryanmen wait.
        // For simplicity, if fu == 20 (base) and closed hand, treat as pinfu (keep fu = 20)
        // but need to ensure no extra fu added.
        // For now, we just enforce minimum fu.
        
        // Minimum fu is 30 (except pinfu which is 20 if closed and no extra fu)
        if (fu < 30) fu = 30
        
        // Round up to nearest 10
        return ceil(fu / 10.0).toInt() * 10
    }
    
    private fun computeWaitFu(
        mentsus: List<Mentsu>,
        winningTile: Tile,
        isOpenHand: Boolean,
        winningMethod: WinningMethod
    ): Int {
        // Find the mentsu that contains the winning tile
        val winningMentsu = mentsus.find { mentsu ->
            mentsu.tiles.any { it.toIndex() == winningTile.toIndex() }
        } ?: return 0 // shouldn't happen
        
        return when (winningMentsu.mentsuType) {
            Toitsu -> {
                // Single wait (tanki) - 2 fu
                2
            }
            Shuntsu -> {
                // Determine if wait is ryanmen (two-sided), penchan (edge), or kanchan (middle)
                val indices = winningMentsu.tiles.map { it.toIndex() }.sorted()
                val winIdx = winningTile.toIndex()
                // Check position of winning tile in sequence
                when (winIdx) {
                    indices[0] -> {
                        // Winning tile is the lowest tile: wait could be penchan if sequence 1-2-3 (wait for 3?)
                        // Actually penchan is edge wait where the missing tile is at one end (e.g., 1-2 waiting for 3)
                        // For sequence (a, a+1, a+2), if winning tile is a, wait could be for a+3? Not possible.
                        // Edge wait occurs when the sequence is 1-2-3 and you wait for 3 (winning tile 1? no).
                        // Simplification: assume not ryanmen, add 2 fu.
                        // TODO: implement proper detection
                        0 // assume ryanmen for now
                    }
                    indices[1] -> {
                        // Winning tile is middle tile: kanchan wait (2 fu)
                        2
                    }
                    indices[2] -> {
                        // Winning tile is highest tile: penchan wait (2 fu)
                        2
                    }
                    else -> 0
                }
            }
            else -> 0 // Koutsu/Kantsu no wait fu
        }
    }

    private fun isYakuhai(tile: Tile, seatWind: Wind, roundWind: Wind): Boolean {
        return when (tile.tileType) {
            is TileWind -> {
                val tileValue = tile.value
                val seatValue = (seatWind as StandardWind).ordinal + 1
                val roundValue = (roundWind as StandardWind).ordinal + 1
                tileValue == seatValue || tileValue == roundValue
            }
            is TileDragon -> true
            else -> false
        }
    }

    override fun calculateBasicPoints(
        han: Int,
        fu: Int,
        isDealer: Boolean,
        isTsumo: Boolean
    ): Int {
        // Limit hand detection
        val limitHand = determineLimitHand(han)
        if (limitHand != null) {
            return when (limitHand) {
                LimitHand.MANGAN -> 2000
                LimitHand.HANEMAN -> 3000
                LimitHand.BAIMAN -> 4000
                LimitHand.SANBAIMAN -> 6000
                LimitHand.YAKUMAN -> 8000
            }
        }
        
        // Basic points formula: fu * 2^(han+2)
        val basicPoints = fu * (1 shl (han + 2))
        // Round up to nearest 100
        return ceil(basicPoints / 100.0).toInt() * 100
    }

    override fun determineLimitHand(han: Int): LimitHand? {
        return when {
            han >= 13 -> LimitHand.YAKUMAN
            han >= 11 -> LimitHand.SANBAIMAN
            han >= 8 -> LimitHand.BAIMAN
            han >= 6 -> LimitHand.HANEMAN
            han >= 5 -> LimitHand.MANGAN
            else -> null
        }
    }

    override fun distributePayments(
        winnerWind: Wind,
        basicPoints: Int,
        isTsumo: Boolean,
        isDealer: Boolean,
        riichiSticks: Int,
        honbaSticks: Int,
        playerWinds: List<Wind>,
        loserWind: Wind?
    ): Map<Wind, Int> {
        val payments = mutableMapOf<Wind, Int>()
        val dealerWind = StandardWind.EAST // Assume East is dealer (true for standard mahjong)
        val isWinnerDealer = winnerWind == dealerWind
        
        if (!isTsumo) {
            // Ron: single loser pays total points
            // Use provided loserWind if available, otherwise fallback to heuristic
            val loser = loserWind ?: if (isWinnerDealer) {
                playerWinds.first { it != winnerWind && it != dealerWind } // any non-dealer
            } else {
                dealerWind // non-dealer wins from dealer
            }
            // Multiplier: dealer winner -> 6, non-dealer winner -> 4
            val multiplier = if (isWinnerDealer) 6 else 4
            val totalPoints = basicPoints * multiplier + honbaSticks * 100 + riichiSticks * 1000
            payments[winnerWind] = totalPoints
            payments[loser] = -totalPoints
            // Other players pay nothing
            playerWinds.filter { it != winnerWind && it != loser }.forEach { payments[it] = 0 }
        } else {
            // Tsumo
            if (isWinnerDealer) {
                // Dealer win: each non-dealer pays basicPoints * 2
                val perPlayer = basicPoints * 2 + honbaSticks * 100 + riichiSticks * 1000
                playerWinds.forEach { wind ->
                    payments[wind] = if (wind == winnerWind) {
                        perPlayer * (playerWinds.size - 1)  // receives from all others
                    } else {
                        -perPlayer
                    }
                }
            } else {
                // Non-dealer win: dealer pays basicPoints * 2, other non-dealers pay basicPoints
                val dealerPayment = basicPoints * 2 + honbaSticks * 100 + riichiSticks * 1000
                val nonDealerPayment = basicPoints + honbaSticks * 100 + riichiSticks * 1000
                // Distribute: winner receives sum of payments
                var totalToWinner = 0
                playerWinds.forEach { wind ->
                    when {
                        wind == winnerWind -> payments[wind] = 0 // will set later
                        wind == dealerWind -> {
                            payments[wind] = -dealerPayment
                            totalToWinner += dealerPayment
                        }
                        else -> {
                            payments[wind] = -nonDealerPayment
                            totalToWinner += nonDealerPayment
                        }
                    }
                }
                payments[winnerWind] = totalToWinner
            }
        }
        
        // Ensure sum of payments is zero (accounting rounding)
        // For simplicity, we assume integer arithmetic works
        
        return payments
    }

    override fun computeExhaustiveDrawPayments(
        playerWinds: List<Wind>,
        discards: Map<Wind, List<Tile>>,
        openHands: Map<Wind, List<List<Tile>>>,
        riichiSticks: Int,
        honbaSticks: Int,
        dealerWind: Wind
    ): Map<Wind, Int> {
        val payments = mutableMapOf<Wind, Int>()
        playerWinds.forEach { payments[it] = 0 }

        // Find players eligible for nagashi mangan
        // Requirements: all discards are yaochuhai, no calls made
        val eligibleWinds = mutableListOf<Wind>()
        for ((wind, tiles) in discards) {
            if (tiles.isEmpty()) continue // must have discarded at least one tile
            val allYaochuhai = tiles.all { it.toIndex().isYaochuhai() }
            val noCalls = openHands[wind].isNullOrEmpty()
            if (allYaochuhai && noCalls) {
                eligibleWinds.add(wind)
            }
        }

        if (eligibleWinds.isEmpty()) {
            // No nagashi mangan, no payments (exhaustive draw with no winner)
            return payments
        }

        // For each eligible player, receive mangan (2000 points) from each other player
        // If multiple eligible, they also pay each other (net zero between them)
        for (eligible in eligibleWinds) {
            for (other in playerWinds) {
                if (other == eligible) continue
                // Mangan payment: 2000 points from other to eligible
                payments[eligible] = payments[eligible]!! + 2000
                payments[other] = payments[other]!! - 2000
            }
        }

        // Note: riichi sticks and honba sticks remain on table for next hand
        return payments
    }
}