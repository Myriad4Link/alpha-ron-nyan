package xyz.uthofficial.arnyan.env.match

/**
 * Common error message constants used across action implementations.
 * Extracted to eliminate duplication and ensure consistency.
 */
object ErrorMessages {
    // Player state errors
    const val PLAYER_HAS_NO_SEAT = "Player has no seat assigned"
    const val TILE_NOT_IN_HAND = "Tile not in hand"
    
    // Action availability errors
    const val ACTION_NOT_AVAILABLE = "Action not available"
    const val ACTION_NOT_AVAILABLE_WITH_TILE = "Action not available with given tile"
    
    // Discard-related errors
    const val NO_DISCARD_TO_ACTION = "No discard to %s"
    const val CANNOT_ACTION_OWN_DISCARD = "Cannot %s own discard"
    const val TILE_MISMATCH = "Tile mismatch"
    
    // Specific action errors
    const val NO_SEQUENCE_POSSIBLE = "No sequence possible"
    const val NO_MATCHING_TILES_FOR_PON = "No matching tiles for pon"
    const val HAND_NOT_COMPLETE = "Hand not complete"
    const val HAND_HAS_NO_YAKU = "Hand has no yaku"
    const val CHII_ONLY_BY_KAMICHA = "Chii can only be called by left neighbor (kamicha)"
    
    // Draw/tsumo errors
    const val NO_DRAW_TO_TSUMO = "No draw to tsumo"
    const val ONLY_DRAWING_PLAYER_CAN_TSUMO = "Only the drawing player can call tsumo"
    
    // Helper functions to format messages with action names
    fun noDiscardToAction(action: String): String = NO_DISCARD_TO_ACTION.format(action.lowercase())
    fun cannotActionOwnDiscard(action: String): String = CANNOT_ACTION_OWN_DISCARD.format(action.lowercase())
}