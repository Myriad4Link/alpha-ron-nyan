package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.wind.StandardWind
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry
import xyz.uthofficial.arnyan.env.yaku.resolver.StandardFastTileResolver
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardShuntsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKoutsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKantsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardToitsuStrategy
import xyz.uthofficial.arnyan.env.yaku.YakuContext
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.YakuConfiguration
import xyz.uthofficial.arnyan.env.yaku.Yaku

private fun Tile.index(): Int = when (tileType) {
    is xyz.uthofficial.arnyan.env.tile.Dragon -> value - 1  // values 1-3 -> indices 0-2
    is xyz.uthofficial.arnyan.env.tile.Man -> value + 2     // values 1-9 -> indices 3-11
    is xyz.uthofficial.arnyan.env.tile.Pin -> value + 11    // values 1-9 -> indices 12-20
    is xyz.uthofficial.arnyan.env.tile.Sou -> value + 20    // values 1-9 -> indices 21-29
    is xyz.uthofficial.arnyan.env.tile.Wind -> value + 29   // values 1-4 -> indices 30-33
    else -> -1
}

private fun Int.isSameSuitAs(other: Int): Boolean =
    TileTypeRegistry.connectivityMask[this] == TileTypeRegistry.connectivityMask[other]

private fun findSequenceTiles(hand: List<Tile>, subject: Tile): Pair<Tile, Tile>? {
    val subjectIdx = subject.index()
    
    // Group tiles by index
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
                // restore removed tiles
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

private fun findMatchingTiles(hand: List<Tile>, subject: Tile): Pair<Tile, Tile>? {
    val subjectIdx = subject.index()
    val matchingTiles = hand.filter { it.index() == subjectIdx }
    return if (matchingTiles.size >= 2) {
        Pair(matchingTiles[0], matchingTiles[1])
    } else {
        null
    }
}

private fun isCompleteHand(closeHand: List<Tile>, subject: Tile? = null): Boolean {
    val hand = if (subject != null) closeHand + subject else closeHand
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

private fun resolvePartitions(closeHand: List<Tile>, subject: Tile? = null): List<LongArray> {
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

private fun computeMaxHan(yakuConfiguration: YakuConfiguration, context: YakuContext, partitions: List<LongArray>): Int {
    if (partitions.isEmpty()) return 0
    var maxHan = 0
    for (partition in partitions) {
        val yakuList = yakuConfiguration.evaluate(context, listOf(partition))
        val totalHan = yakuList.sumOf { it.second }
        if (totalHan > maxHan) maxHan = totalHan
    }
    return maxHan
}

private fun canWin(observation: MatchObservation, actor: Player, subject: Tile, winningMethod: WinningMethod): Boolean {
    val partitions = resolvePartitions(actor.closeHand, subject)
    if (partitions.isEmpty()) return false
    val seatWind = actor.seat ?: return false
    val roundWind = observation.roundRotationStatus.place
    val isOpenHand = actor.openHand.isNotEmpty()
    val isRiichiDeclared = false // TODO: implement riichi tracking
    val context = YakuContext(
        seatWind = seatWind,
        roundWind = roundWind,
        isOpenHand = isOpenHand,
        isRiichiDeclared = isRiichiDeclared,
        winningTile = subject,
        winningMethod = winningMethod
    )
    val maxHan = computeMaxHan(observation.yakuConfiguration, context, partitions)
    return maxHan > 0
}

object Chii : Action {
    override val id = Action.ID_CHII
    override fun toString() = "CHII"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        // Chii can only be called on the last discarded tile
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false
        
        // Chii can only be called by the player to the left of the discarding player (kamicha)
        val discardingPlayer = lastAction.player
        val discardingSeat = discardingPlayer.seat ?: return false
        val actorSeat = actor.seat ?: return false
        
        // Actor must be kamicha (left neighbor) of discarding player
        val kamicha = when (val result = observation.topology.getKamicha(discardingSeat)) {
            is Result.Success -> result.value
            is Result.Failure -> null
        } ?: return false
        if (actorSeat != kamicha) return false
        
        // Check if actor's hand can form a sequence with subject
        return findSequenceTiles(actor.closeHand, subject) != null
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val lastAction = observation.lastAction
        val actorSeat = actor.seat ?: Result.Failure<ActionError>(
            MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT).wrapActionError()
        ).bind()
        
        if (lastAction !is LastAction.Discard) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.noDiscardToAction("chii")).wrapActionError()).bind()
        }
        if (lastAction.tile != subject) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.TILE_MISMATCH).wrapActionError()).bind()
        }
        
        val discardingPlayer = lastAction.player
        val discardingSeat = discardingPlayer.seat ?: StandardWind.EAST
        if (discardingPlayer == actor) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.cannotActionOwnDiscard("chii")).wrapActionError()).bind()
        }
        
        // Actor must be kamicha (left neighbor) of discarding player
        val kamicha = observation.topology.getKamicha(discardingSeat).wrapActionError().bind()
        if (actorSeat != kamicha) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.CHII_ONLY_BY_KAMICHA).wrapActionError()).bind()
        }
        
        val sequenceTiles = findSequenceTiles(actor.closeHand, subject)
            ?: Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.NO_SEQUENCE_POSSIBLE).wrapActionError()).bind()
        
        val (tile1, tile2) = sequenceTiles
        
        // Create state changes instead of mutating
        val openGroup = listOf(tile1, tile2, subject).sortedBy { it.index() }
        val stateChanges = listOf(
            StateChange.RemoveTilesFromHand(actorSeat, listOf(tile1, tile2)),
            StateChange.AddOpenGroup(actorSeat, openGroup),
            StateChange.RemoveTileFromDiscards(discardingSeat, subject)
        )
        
        // Remove subject from discards of the player who discarded it
        val currentDiscards = observation.discards.toMutableMap()
        val playerDiscards = currentDiscards[discardingSeat]?.toMutableList() ?: mutableListOf()
        // Remove the last occurrence (most recent discard)
        val lastIndex = playerDiscards.indexOfLast { it == subject }
        if (lastIndex != -1) {
            playerDiscards.removeAt(lastIndex)
        }
        currentDiscards[discardingSeat] = playerDiscards
        
        // Turn passes to the actor who called Chii
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = actorSeat,
            roundRotationStatus = observation.roundRotationStatus,
            discards = currentDiscards,
            lastAction = LastAction.Chii(subject, actor),
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, actorSeat, false, stateChanges)
    }
}

object Pon : Action {
    override val id = Action.ID_PON
    override fun toString() = "PON"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        // Pon can only be called on the last discarded tile
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false
        
        // Cannot call pon on own discard
        if (lastAction.player == actor) return false
        
        // Check if actor's hand contains two matching tiles
        return findMatchingTiles(actor.closeHand, subject) != null
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val lastAction = observation.lastAction
        val actorSeat = actor.seat ?: Result.Failure<ActionError>(
            MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT).wrapActionError()
        ).bind()
        
        if (lastAction !is LastAction.Discard) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.noDiscardToAction("pon")).wrapActionError()).bind()
        }
        if (lastAction.tile != subject) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.TILE_MISMATCH).wrapActionError()).bind()
        }
        
        val discardingPlayer = lastAction.player
        val discardingSeat = discardingPlayer.seat ?: StandardWind.EAST
        if (discardingPlayer == actor) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.cannotActionOwnDiscard("pon")).wrapActionError()).bind()
        }
        
        val matchingTiles = findMatchingTiles(actor.closeHand, subject)
            ?: Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.NO_MATCHING_TILES_FOR_PON).wrapActionError()).bind()
        
        val (tile1, tile2) = matchingTiles
        
        // Create state changes instead of mutating
        val openGroup = listOf(tile1, tile2, subject).sortedBy { it.index() }
        val stateChanges = listOf(
            StateChange.RemoveTilesFromHand(actorSeat, listOf(tile1, tile2)),
            StateChange.AddOpenGroup(actorSeat, openGroup),
            StateChange.RemoveTileFromDiscards(discardingSeat, subject)
        )
        
        // Remove subject from discards of the player who discarded it
        val currentDiscards = observation.discards.toMutableMap()
        val playerDiscards = currentDiscards[discardingSeat]?.toMutableList() ?: mutableListOf()
        // Remove the last occurrence (most recent discard)
        val lastIndex = playerDiscards.indexOfLast { it == subject }
        if (lastIndex != -1) {
            playerDiscards.removeAt(lastIndex)
        }
        currentDiscards[discardingSeat] = playerDiscards
        
        // Turn passes to the actor who called Pon
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = actorSeat,
            roundRotationStatus = observation.roundRotationStatus,
            discards = currentDiscards,
            lastAction = LastAction.Pon(subject, actor),
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, actorSeat, false, stateChanges)
    }
}

object Ron : Action {
    override val id = Action.ID_RON
    override fun toString() = "RON"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        // Ron can only be called on the last discarded tile
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        if (lastAction.tile != subject) return false
        
        // Cannot call ron on own discard (that would be tsumo? Actually self-draw win is tsumo)
        if (lastAction.player == actor) return false
        
        // Check if hand is complete with this tile and has at least one yaku
        return canWin(observation, actor, subject, WinningMethod.RON)
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val lastAction = observation.lastAction
        val actorSeat = actor.seat ?: Result.Failure<ActionError>(
            MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT).wrapActionError()
        ).bind()
        
        if (lastAction !is LastAction.Discard) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.noDiscardToAction("ron")).wrapActionError()).bind()
        }
        if (lastAction.tile != subject) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.TILE_MISMATCH).wrapActionError()).bind()
        }
        
        val discardingPlayer = lastAction.player
        val discardingSeat = discardingPlayer.seat ?: StandardWind.EAST
        if (discardingPlayer == actor) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.cannotActionOwnDiscard("ron")).wrapActionError()).bind()
        }
        
        val partitions = resolvePartitions(actor.closeHand, subject)
        if (partitions.isEmpty()) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.HAND_NOT_COMPLETE).wrapActionError()).bind()
        }
        val seatWind = actor.seat ?: StandardWind.EAST
        val roundWind = observation.roundRotationStatus.place
        val isOpenHand = actor.openHand.isNotEmpty()
        val isRiichiDeclared = false // TODO: implement riichi tracking
        val context = YakuContext(
            seatWind = seatWind,
            roundWind = roundWind,
            isOpenHand = isOpenHand,
            isRiichiDeclared = isRiichiDeclared,
            winningTile = subject,
            winningMethod = WinningMethod.RON
        )
        val maxHan = computeMaxHan(observation.yakuConfiguration, context, partitions)
        if (maxHan == 0) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.HAND_HAS_NO_YAKU).wrapActionError()).bind()
        }
        
        // Remove subject from discards of the player who discarded it
        val currentDiscards = observation.discards.toMutableMap()
        val playerDiscards = currentDiscards[discardingSeat]?.toMutableList() ?: mutableListOf()
        // Remove the last occurrence (most recent discard)
        val lastIndex = playerDiscards.indexOfLast { it == subject }
        if (lastIndex != -1) {
            playerDiscards.removeAt(lastIndex)
        }
        currentDiscards[discardingSeat] = playerDiscards
        
        val stateChanges = listOf(
            StateChange.RemoveTileFromDiscards(discardingSeat, subject)
        )
        
        // Game ends with Ron
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = actorSeat,
            roundRotationStatus = observation.roundRotationStatus,
            discards = currentDiscards,
            lastAction = LastAction.Ron(subject, actor),
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, actorSeat, true, stateChanges)
    }
}

object TsuMo : Action {
    override val id = Action.ID_TSUMO
    override fun toString() = "TSU_MO"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        // TsuMo can only be called on the last drawn tile
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        if (lastAction.tile != subject) return false
        
        // Only the player who drew the tile can call tsumo
        if (lastAction.player != actor) return false
        
        // Check if hand is complete with this tile and has at least one yaku
        return canWin(observation, actor, subject, WinningMethod.TSUMO)
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val lastAction = observation.lastAction
        val actorSeat = actor.seat ?: Result.Failure<ActionError>(
            MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT).wrapActionError()
        ).bind()
        
        if (lastAction !is LastAction.Draw) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.NO_DRAW_TO_TSUMO).wrapActionError()).bind()
        }
        if (lastAction.tile != subject) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.TILE_MISMATCH).wrapActionError()).bind()
        }
        
        if (lastAction.player != actor) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.ONLY_DRAWING_PLAYER_CAN_TSUMO).wrapActionError()).bind()
        }
        
        val partitions = resolvePartitions(actor.closeHand, subject)
        if (partitions.isEmpty()) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.HAND_NOT_COMPLETE).wrapActionError()).bind()
        }
        val seatWind = actor.seat ?: StandardWind.EAST
        val roundWind = observation.roundRotationStatus.place
        val isOpenHand = actor.openHand.isNotEmpty()
        val isRiichiDeclared = false // TODO: implement riichi tracking
        val context = YakuContext(
            seatWind = seatWind,
            roundWind = roundWind,
            isOpenHand = isOpenHand,
            isRiichiDeclared = isRiichiDeclared,
            winningTile = subject,
            winningMethod = WinningMethod.TSUMO
        )
        val maxHan = computeMaxHan(observation.yakuConfiguration, context, partitions)
        if (maxHan == 0) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), actorSeat, ErrorMessages.HAND_HAS_NO_YAKU).wrapActionError()).bind()
        }
        
        // Game ends with TsuMo
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = actorSeat,
            roundRotationStatus = observation.roundRotationStatus,
            discards = observation.discards,
            lastAction = LastAction.TsuMo(subject, actor),
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, actorSeat, true, emptyList())
    }
}

object DiscardAction : Action {
    override val id = Action.ID_DISCARD
    override fun toString() = "DISCARD"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        return actor.seat == observation.currentSeatWind && actor.closeHand.contains(subject)
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val seat = actor.seat ?: Result.Failure<ActionError>(
            MatchError.ActionNotAvailable(
                toString(),
                StandardWind.EAST,
                ErrorMessages.PLAYER_HAS_NO_SEAT
            ).wrapActionError()
        ).bind()
        if (seat != observation.currentSeatWind) {
            Result.Failure<ActionError>(MatchError.NotPlayersTurn(seat, observation.currentSeatWind).wrapActionError()).bind()
        }
        val seatWind: Wind = seat
        if (!actor.closeHand.contains(subject)) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), seatWind, ErrorMessages.TILE_NOT_IN_HAND).wrapActionError()).bind()
        }
        
        val stateChanges = listOf(
            StateChange.RemoveTilesFromHand(seatWind, listOf(subject))
        )
        
        val currentDiscards = observation.discards
        val newDiscards = currentDiscards.toMutableMap()
        newDiscards[seatWind] = currentDiscards.getOrDefault(seatWind, emptyList()) + subject
        
        // Keep current seat wind as discarding player until interrupts resolved
        val nextWind = seatWind
        
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = seatWind,
            roundRotationStatus = observation.roundRotationStatus,
            discards = newDiscards,
            lastAction = LastAction.Discard(subject, actor),
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, nextWind, false, stateChanges)
    }
}

object PassAction : Action {
    override val id = Action.ID_PASS
    override fun toString() = "PASS"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        // Pass is only available when there is a discard to respond to
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) return false
        
        // Subject must be the discarded tile
        if (lastAction.tile != subject) return false
        
        // Player must have at least one interrupt action (Chii, Pon, Ron) available
        // This will be determined by the engine based on player's action mask
        // We'll return true here; engine will filter based on actual availability
        return true
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> = binding {
        val seat = actor.seat
        if (seat == null) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT).wrapActionError()).bind()
        }
        val seatWind: Wind = seat
        
        // Ensure we're passing on the correct discard
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Discard) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), seatWind, ErrorMessages.noDiscardToAction("pass")).wrapActionError()).bind()
        }
        if (lastAction.tile != subject) {
            Result.Failure<ActionError>(MatchError.ActionNotAvailable(toString(), seatWind, ErrorMessages.TILE_MISMATCH).wrapActionError()).bind()
        }
        
        // No state changes needed, keep last action as discard (interrupt phase continues)
        val newObservation = MatchObservation(
            players = observation.players,
            wall = observation.wall,
            topology = observation.topology,
            currentSeatWind = observation.currentSeatWind,
            roundRotationStatus = observation.roundRotationStatus,
            discards = observation.discards,
            lastAction = observation.lastAction,  // Keep as Discard
            yakuConfiguration = observation.yakuConfiguration
        )
        
        StepResult(newObservation, observation.currentSeatWind, false, emptyList())
    }
}