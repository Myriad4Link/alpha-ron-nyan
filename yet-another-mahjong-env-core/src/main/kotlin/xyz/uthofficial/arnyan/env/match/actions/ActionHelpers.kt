package xyz.uthofficial.arnyan.env.match.actions

import xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.StateChange
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.yaku.resolver.CompactMentsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Kantsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Koutsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Shuntsu
import xyz.uthofficial.arnyan.env.yaku.resolver.Toitsu
import xyz.uthofficial.arnyan.env.yaku.resolver.StandardFastTileResolver
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKantsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKoutsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardShuntsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardToitsuStrategy
import xyz.uthofficial.arnyan.env.yaku.tenpai.StandardFastTenpaiEvaluator

internal fun Tile.index(): Int = when (tileType) {
    is xyz.uthofficial.arnyan.env.tile.Dragon -> value - 1
    is xyz.uthofficial.arnyan.env.tile.Man -> value + 2
    is xyz.uthofficial.arnyan.env.tile.Pin -> value + 11
    is xyz.uthofficial.arnyan.env.tile.Sou -> value + 20
    is xyz.uthofficial.arnyan.env.tile.Wind -> value + 29
    else -> -1
}

internal fun Int.isSameSuitAs(other: Int): Boolean =
    TileTypeRegistry.connectivityMask[this] == TileTypeRegistry.connectivityMask[other]

internal fun findSequenceTiles(hand: List<Tile>, subject: Tile): Pair<Tile, Tile>? {
    val subjectIdx = subject.index()

    val indexToTiles = hand.groupBy { it.index() }.mapValues { (_, tiles) -> tiles.toMutableList() }.toMutableMap()

    val patterns = listOf(
        listOf(subjectIdx - 2, subjectIdx - 1, subjectIdx),
        listOf(subjectIdx - 1, subjectIdx, subjectIdx + 1),
        listOf(subjectIdx, subjectIdx + 1, subjectIdx + 2)
    )

    for (pattern in patterns) {
        if (pattern.any { it < 0 || it >= TileTypeRegistry.SIZE }) continue
        if (!pattern.all { it.isSameSuitAs(subjectIdx) }) continue

        val needed = pattern.filter { it != subjectIdx }
        val available = mutableListOf<Tile>()
        for (idx in needed) {
            val tiles = indexToTiles[idx]
            if (!tiles.isNullOrEmpty()) {
                available.add(tiles.removeAt(0))
            } else {
                needed.forEachIndexed { i, iIdx ->
                    if (i < available.size) {
                        indexToTiles.getOrPut(iIdx) { mutableListOf() }.add(available[i])
                    }
                }
                break
            }
        }
        if (available.size == 2) {
            return Pair(available[0], available[1])
        }
    }
    return null
}

internal fun findMatchingTiles(hand: List<Tile>, subject: Tile): Pair<Tile, Tile>? {
    val subjectIdx = subject.index()
    val matchingTiles = hand.filter { it.index() == subjectIdx }
    return if (matchingTiles.size >= 2) {
        Pair(matchingTiles[0], matchingTiles[1])
    } else {
        null
    }
}

internal fun isCompleteHand(closeHand: List<Tile>, subject: Tile? = null): Boolean {
    val hand = if (subject != null) closeHand + subject else closeHand
    if (hand.size != 14) return false
    val histogram = IntArray(TileTypeRegistry.SIZE)
    TileTypeRegistry.getHistogram(hand, histogram)
    val resolver = StandardFastTileResolver(
        StandardShuntsuStrategy,
        StandardKoutsuStrategy,
        StandardKantsuStrategy,
        StandardToitsuStrategy
    )
    val partitions = resolver.resolve(histogram)
    return partitions.isNotEmpty()
}

internal fun resolvePartitions(closeHand: List<Tile>, subject: Tile? = null): List<LongArray> {
    val hand = if (subject != null) closeHand + subject else closeHand
    val histogram = IntArray(TileTypeRegistry.SIZE)
    TileTypeRegistry.getHistogram(hand, histogram)
    val resolver = StandardFastTileResolver(
        StandardShuntsuStrategy,
        StandardKoutsuStrategy,
        StandardKantsuStrategy,
        StandardToitsuStrategy
    )
    return resolver.resolve(histogram)
}

internal fun computeMaxHan(
    yakuConfiguration: xyz.uthofficial.arnyan.env.yaku.YakuConfiguration,
    context: YakuContext,
    partitions: List<LongArray>,
    openMentsus: LongArray = longArrayOf()
): Int {
    if (partitions.isEmpty()) return 0
    var maxHan = 0
    for (partition in partitions) {
        val fullPartition = partition + openMentsus
        val yakuList = yakuConfiguration.evaluate(context, listOf(fullPartition))
        val totalHan = yakuList.sumOf { it.second }
        if (totalHan > maxHan) maxHan = totalHan
    }
    return maxHan
}

internal fun computeBestPartition(
    yakuConfiguration: xyz.uthofficial.arnyan.env.yaku.YakuConfiguration,
    context: YakuContext,
    partitions: List<LongArray>
): Pair<LongArray, Int>? {
    if (partitions.isEmpty()) return null
    var bestPartition: LongArray? = null
    var maxHan = 0
    for (partition in partitions) {
        val yakuList = yakuConfiguration.evaluate(context, listOf(partition))
        val totalHan = yakuList.sumOf { it.second }
        if (totalHan > maxHan) {
            maxHan = totalHan
            bestPartition = partition
        }
    }
    return if (bestPartition != null) bestPartition to maxHan else null
}

internal fun tileGroupToMentsu(tiles: List<Tile>, isOpen: Boolean): CompactMentsu {
    val indices = tiles.map { it.index() }.toIntArray()
    val sorted = indices.sorted()
    val mentsuTypeIndex = when (tiles.size) {
        2 -> {
            if (sorted[0] == sorted[1]) MentsuTypeRegistry.getIndex(Toitsu)
            else error("Invalid pair")
        }
        3 -> {
            if (sorted[0] == sorted[1] && sorted[1] == sorted[2]) {
                MentsuTypeRegistry.getIndex(Koutsu)
            } else if (sorted[0] + 1 == sorted[1] && sorted[1] + 1 == sorted[2] &&
                sorted.all { TileTypeRegistry.connectivityMask[it] == TileTypeRegistry.connectivityMask[sorted[0]] }) {
                MentsuTypeRegistry.getIndex(Shuntsu)
            } else {
                error("Invalid triplet")
            }
        }
        4 -> {
            if (sorted[0] == sorted[1] && sorted[1] == sorted[2] && sorted[2] == sorted[3]) {
                MentsuTypeRegistry.getIndex(Kantsu)
            } else {
                error("Invalid quadruplet")
            }
        }
        else -> error("Invalid tile group size")
    }
    val containsYaochuhai = indices.any { TileTypeRegistry.yaochuhaiIndices.contains(it) }
    return CompactMentsu(CompactMentsu.pack(
        tileIndices = sorted.toIntArray(),
        mentsuTypeIndex = mentsuTypeIndex,
        isOpen = isOpen,
        containsYaochuhai = containsYaochuhai
    ))
}

internal fun computeScoringStateChanges(
    observation: MatchObservation,
    actor: Player,
    subject: Tile,
    winningMethod: WinningMethod,
    partitions: List<LongArray>,
    discardingSeat: Wind? = null
): List<StateChange> {
    val bestPartition = computeBestPartition(observation.yakuConfiguration, 
        YakuContext(
            seatWind = actor.seat ?: return emptyList(),
            roundWind = observation.roundRotationStatus.place,
            isOpenHand = actor.openHand.isNotEmpty(),
            isRiichiDeclared = actor.isRiichiDeclared,
            winningTile = subject,
            winningMethod = winningMethod
        ),
        partitions
    ) ?: return emptyList()
    
    val (partition, han) = bestPartition
    val closeMentsus = partition.map { CompactMentsu(it) }
    val openMentsus = actor.openHand.flatMap { group ->
        listOf(tileGroupToMentsu(group, isOpen = true))
    }
    val allMentsus = closeMentsus + openMentsus
    
    val fu = observation.scoringCalculator.calculateFu(
        hand = actor.closeHand + subject,
        mentsus = allMentsus,
        context = YakuContext(
            seatWind = actor.seat!!,
            roundWind = observation.roundRotationStatus.place,
            isOpenHand = actor.openHand.isNotEmpty(),
            isRiichiDeclared = actor.isRiichiDeclared,
            winningTile = subject,
            winningMethod = winningMethod
        ),
        winnerWind = actor.seat!!,
        isDealer = actor.seat == observation.roundRotationStatus.place
    )
    
    val nukiHan = actor.nukiCount
    val totalHan = han + nukiHan
    
    val isDealer = actor.seat == observation.roundRotationStatus.place
    val basicPoints = observation.scoringCalculator.calculateBasicPoints(
        han = totalHan,
        fu = fu,
        isDealer = isDealer,
        isTsumo = winningMethod == WinningMethod.TSUMO
    )
    
    val payments = observation.scoringCalculator.distributePayments(
        winnerWind = actor.seat!!,
        basicPoints = basicPoints,
        isTsumo = winningMethod == WinningMethod.TSUMO,
        isDealer = isDealer,
        riichiSticks = observation.riichiSticks,
        honbaSticks = observation.honbaSticks,
        playerWinds = observation.topology.seats,
        loserWind = discardingSeat
    )
    
    val stateChanges = mutableListOf<StateChange>()
    payments.forEach { (wind, delta) ->
        if (delta != 0) {
            stateChanges.add(StateChange.UpdatePlayerScore(wind, delta))
        }
    }
    if (observation.riichiSticks > 0) {
        stateChanges.add(StateChange.UpdatePlayerScore(actor.seat!!, observation.riichiSticks * 1000))
        stateChanges.add(StateChange.UpdateRiichiSticks(-observation.riichiSticks))
    }
    if (observation.honbaSticks > 0) {
        stateChanges.add(StateChange.UpdatePlayerScore(actor.seat!!, observation.honbaSticks * 100))
        stateChanges.add(StateChange.UpdateHonbaSticks(-observation.honbaSticks))
    }
    
    return stateChanges
}

internal fun canWin(observation: MatchObservation, actor: Player, subject: Tile, winningMethod: WinningMethod): Boolean {
    val partitions = resolvePartitions(actor.closeHand, subject)
    if (partitions.isEmpty()) return false
    val seatWind = actor.seat ?: return false
    val roundWind = observation.roundRotationStatus.place
    val isOpenHand = actor.openHand.isNotEmpty()
    val isRiichiDeclared = actor.isRiichiDeclared
    val context = YakuContext(
        seatWind = seatWind,
        roundWind = roundWind,
        isOpenHand = isOpenHand,
        isRiichiDeclared = isRiichiDeclared,
        winningTile = subject,
        winningMethod = winningMethod
    )
    val openMentsus = actor.openHand.map { tileGroupToMentsu(it, isOpen = true) }
        .map { it.raw }.toLongArray()
    var maxHan = 0
    for (partition in partitions) {
        val fullPartition = partition + openMentsus
        val yakuList = observation.yakuConfiguration.evaluate(context, listOf(fullPartition))
        val totalHan = yakuList.sumOf { it.second }
        if (totalHan > maxHan) maxHan = totalHan
    }
    val nukiHan = actor.nukiCount
    return (maxHan + nukiHan) > 0
}

internal fun isInTenpai(closeHand: List<Tile>, discardTile: Tile): Boolean {
    if (closeHand.size == 13) {
        val histogram = IntArray(TileTypeRegistry.SIZE)
        TileTypeRegistry.getHistogram(closeHand, histogram)
        
        val evaluator = StandardFastTenpaiEvaluator(
            StandardFastTileResolver(
                StandardShuntsuStrategy,
                StandardKoutsuStrategy,
                StandardKantsuStrategy,
                StandardToitsuStrategy
            )
        )
        
        val tenpaiResult = evaluator.evaluate(histogram)
        return tenpaiResult.isNotEmpty()
    } else {
        val histogram = IntArray(TileTypeRegistry.SIZE)
        TileTypeRegistry.getHistogram(closeHand, histogram)
        val segment = TileTypeRegistry.getSegment(discardTile.tileType)
        val index = discardTile.value + segment[0] - 1
        histogram[index]--
        
        val evaluator = StandardFastTenpaiEvaluator(
            StandardFastTileResolver(
                StandardShuntsuStrategy,
                StandardKoutsuStrategy,
                StandardKantsuStrategy,
                StandardToitsuStrategy
            )
        )
        
        val tenpaiResult = evaluator.evaluate(histogram)
        return tenpaiResult.isNotEmpty()
    }
}

internal fun getTenpaiWaitingTiles(closeHand: List<Tile>): List<Int> {
    val histogram = IntArray(TileTypeRegistry.SIZE)
    TileTypeRegistry.getHistogram(closeHand, histogram)
    
    val evaluator = StandardFastTenpaiEvaluator(
        StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
    )
    
    val tenpaiResult = evaluator.evaluate(histogram)
    return tenpaiResult.keys.toList()
}
