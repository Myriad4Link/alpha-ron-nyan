package xyz.uthofficial.arnyan.simplified

import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.index.NDIndex
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.yaku.resolver.StandardFastTileResolver
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKantsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardKoutsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardShuntsuStrategy
import xyz.uthofficial.arnyan.env.yaku.resolver.strategies.StandardToitsuStrategy
import xyz.uthofficial.arnyan.env.yaku.tenpai.StandardFastTenpaiEvaluator
import xyz.uthofficial.arnyan.env.generated.TileTypeRegistry

/**
 * Action Encoder for Sanma (3-player Mahjong).
 * 
 * Encodes the agent's chosen action into a flat NDArray suitable for:
 * - Training targets (supervised learning / behavior cloning)
 * - Replay buffer storage
 * - Policy output comparison (loss calculation)
 * 
 * Output shape: [38]
 * - Indices [0-26]: Subject tile section (one-hot encoding of the tile being acted on)
 * - Indices [27-37]: Action type section (one-hot encoding of the action type chosen)
 * 
 * Action type mapping:
 * [27] = Chii    [28] = Pon     [29] = Ron     [30] = Tsumo
 * [31] = Discard [32] = Pass    [33] = Riichi  [34] = Ankan
 * [35] = Minkan  [36] = Kakan   [37] = Nuki
 * 
 * Validation is performed BEFORE encoding to ensure:
 * 1. The action type is in observation.availableActions
 * 2. The subject tile is valid for the action type
 * 
 * @throws ActionEncodingError if validation fails
 */
class SanmaActionEncoder {
    
    companion object {
        const val SUBJECT_COUNT = 27
        const val ACTION_COUNT = 11
        const val TOTAL_SIZE = 38
        const val ACTION_OFFSET = 27
        
        const val IDX_CHII = 27
        const val IDX_PON = 28
        const val IDX_RON = 29
        const val IDX_TSUMO = 30
        const val IDX_DISCARD = 31
        const val IDX_PASS = 32
        const val IDX_RIICHI = 33
        const val IDX_ANKAN = 34
        const val IDX_MINKAN = 35
        const val IDX_KAKAN = 36
        const val IDX_NUKI = 37
    }
    
    private val tenpaiEvaluator = StandardFastTenpaiEvaluator(
        StandardFastTileResolver(
            StandardShuntsuStrategy,
            StandardKoutsuStrategy,
            StandardKantsuStrategy,
            StandardToitsuStrategy
        )
    )
    
    /**
     * Encodes the agent's chosen action into a flat NDArray.
     * 
     * @param manager NDManager for creating NDArray
     * @param actionType The action type chosen (e.g., DiscardAction, Pon)
     * @param subjectTile The tile being acted on (null for Pass)
     * @param observation Current match observation (for validation)
     * @param player The player performing the action
     * @return NDArray of shape [38] with one-hot encoding
     * @throws ActionEncodingError if action or subject is invalid
     */
    fun encode(
        manager: NDManager,
        actionType: Action,
        subjectTile: Tile?,
        observation: MatchObservation,
        player: Player
    ): NDArray {
        validate(actionType, subjectTile, observation, player)
        
        val encoded = manager.zeros(Shape(TOTAL_SIZE.toLong()), DataType.FLOAT32)
        
        encodeSubjectTile(encoded, actionType, subjectTile)
        encodeActionType(encoded, actionType)
        
        return encoded
    }
    
    /**
     * Validates the action before encoding.
     * 
     * @param actionType The action type to validate
     * @param subjectTile The subject tile to validate (null for Pass)
     * @param observation Current match observation
     * @param player The player performing the action
     * @throws ActionEncodingError if validation fails
     */
    fun validate(
        actionType: Action,
        subjectTile: Tile?,
        observation: MatchObservation,
        player: Player
    ) {
        if (actionType !in observation.availableActions) {
            throw ActionEncodingError.ActionNotAvailable(actionType, observation.availableActions)
        }
        
        when (actionType.id) {
            Action.ID_PASS -> {
                if (subjectTile != null) {
                    throw ActionEncodingError.PassWithSubject(subjectTile)
                }
            }
            
            Action.ID_DISCARD, Action.ID_RIICHI -> {
                if (subjectTile == null) {
                    throw ActionEncodingError.MissingSubjectTile(actionType)
                }
                if (subjectTile !in player.closeHand) {
                    throw ActionEncodingError.SubjectTileNotInHand(subjectTile, actionType)
                }
                
                if (actionType.id == Action.ID_RIICHI) {
                    val remainingHand = player.closeHand - subjectTile
                    val histogram = IntArray(TileTypeRegistry.SIZE)
                    TileTypeRegistry.getHistogram(remainingHand, histogram)
                    val tenpaiTiles = tenpaiEvaluator.evaluate(histogram)
                    if (tenpaiTiles.isEmpty()) {
                        throw ActionEncodingError.RiichiNotInTenpai(subjectTile)
                    }
                }
            }
            
            Action.ID_CHII, Action.ID_PON, Action.ID_RON, Action.ID_MINKAN -> {
                if (subjectTile == null) {
                    throw ActionEncodingError.MissingSubjectTile(actionType)
                }
                val lastAction = observation.lastAction as? LastAction.Discard
                    ?: throw ActionEncodingError.SubjectTileMismatch(
                        subjectTile,
                        Tile(xyz.uthofficial.arnyan.env.tile.Dragon, 1),
                        actionType
                    )
                if (subjectTile != lastAction.tile) {
                    throw ActionEncodingError.SubjectTileMismatch(subjectTile, lastAction.tile, actionType)
                }
            }
            
            Action.ID_TSUMO, Action.ID_ANKAN, Action.ID_KAKAN, Action.ID_NUKI -> {
                if (subjectTile == null) {
                    throw ActionEncodingError.MissingSubjectTile(actionType)
                }
                val lastAction = observation.lastAction as? LastAction.Draw
                    ?: throw ActionEncodingError.SubjectTileMismatch(
                        subjectTile,
                        Tile(xyz.uthofficial.arnyan.env.tile.Dragon, 1),
                        actionType
                    )
                if (subjectTile != lastAction.tile) {
                    throw ActionEncodingError.SubjectTileMismatch(subjectTile, lastAction.tile, actionType)
                }
            }
            
            else -> {
                throw ActionEncodingError.ActionNotAvailable(actionType, observation.availableActions)
            }
        }
    }
    
    /**
     * Encodes the subject tile into the subject section [0-26].
     */
    private fun encodeSubjectTile(
        encoded: NDArray,
        actionType: Action,
        subjectTile: Tile?
    ) {
        if (actionType.id == Action.ID_PASS) {
            return
        }
        
        if (subjectTile == null) {
            return
        }
        
        val registryIndex = subjectTile.index()
        val sanmaIndex = SanmaTileMapping.registryToSanma(registryIndex)
            ?: return
        
        encoded.set(NDIndex(sanmaIndex.toString()), 1.0f)
    }
    
    /**
     * Encodes the action type into the action section [27-37].
     */
    private fun encodeActionType(
        encoded: NDArray,
        actionType: Action
    ) {
        val actionIndex = when (actionType.id) {
            Action.ID_CHII -> IDX_CHII
            Action.ID_PON -> IDX_PON
            Action.ID_RON -> IDX_RON
            Action.ID_TSUMO -> IDX_TSUMO
            Action.ID_DISCARD -> IDX_DISCARD
            Action.ID_PASS -> IDX_PASS
            Action.ID_RIICHI -> IDX_RIICHI
            Action.ID_ANKAN -> IDX_ANKAN
            Action.ID_MINKAN -> IDX_MINKAN
            Action.ID_KAKAN -> IDX_KAKAN
            Action.ID_NUKI -> IDX_NUKI
            else -> return
        }
        
        encoded.set(NDIndex(actionIndex.toString()), 1.0f)
    }
}

private fun Tile.index(): Int = when (tileType) {
    is xyz.uthofficial.arnyan.env.tile.Dragon -> value - 1
    is xyz.uthofficial.arnyan.env.tile.Man -> value + 2
    is xyz.uthofficial.arnyan.env.tile.Pin -> value + 11
    is xyz.uthofficial.arnyan.env.tile.Sou -> value + 20
    is xyz.uthofficial.arnyan.env.tile.Wind -> value + 29
    else -> -1
}
