package xyz.uthofficial.arnyan.simplified

/**
 * Error types for action encoding failures.
 * 
 * These errors occur when the agent's chosen action cannot be encoded
 * due to validation failures (invalid action type or subject tile).
 */
sealed class ActionEncodingError : Exception() {
    
    /**
     * The chosen action is not in the list of available actions.
     */
    data class ActionNotAvailable(
        val action: xyz.uthofficial.arnyan.env.match.Action,
        val availableActions: List<xyz.uthofficial.arnyan.env.match.Action>
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "ActionNotAvailable: $action is not in available actions $availableActions"
    }
    
    /**
     * The subject tile is not in the player's closed hand (required for Discard/Riichi).
     */
    data class SubjectTileNotInHand(
        val tile: xyz.uthofficial.arnyan.env.tile.Tile,
        val action: xyz.uthofficial.arnyan.env.match.Action
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "SubjectTileNotInHand: $tile not in hand for action $action"
    }
    
    /**
     * The subject tile doesn't match the expected tile for the action.
     * For Chii/Pon/Ron/Minkan: must match the discarded tile.
     * For Ankan/Kakan/Tsumo/Nuki: must match the drawn tile.
     */
    data class SubjectTileMismatch(
        val provided: xyz.uthofficial.arnyan.env.tile.Tile,
        val expected: xyz.uthofficial.arnyan.env.tile.Tile,
        val action: xyz.uthofficial.arnyan.env.match.Action
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "SubjectTileMismatch: provided $provided, expected $expected for action $action"
    }
    
    /**
     * Pass action was provided with a non-null subject tile.
     * Pass should have no subject tile.
     */
    data class PassWithSubject(
        val subject: xyz.uthofficial.arnyan.env.tile.Tile
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "PassWithSubject: Pass action should not have a subject tile, got $subject"
    }
    
    /**
     * Riichi action would not leave the hand in tenpai.
     */
    data class RiichiNotInTenpai(
        val tile: xyz.uthofficial.arnyan.env.tile.Tile
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "RiichiNotInTenpai: discarding $tile does not leave hand in tenpai"
    }
    
    /**
     * Subject tile is null when it should not be (for actions other than Pass).
     */
    data class MissingSubjectTile(
        val action: xyz.uthofficial.arnyan.env.match.Action
    ) : ActionEncodingError() {
        override fun toString(): String = 
            "MissingSubjectTile: $action requires a subject tile"
    }
}
