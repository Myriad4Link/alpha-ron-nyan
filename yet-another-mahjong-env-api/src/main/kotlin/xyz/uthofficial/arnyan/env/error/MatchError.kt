package xyz.uthofficial.arnyan.env.error

import xyz.uthofficial.arnyan.env.wind.Wind

sealed interface MatchError : ArnyanError {
    data class ActionNotAvailable(val actionName: String, val playerSeat: Wind, val reason: String?) : MatchError
    data class NotPlayersTurn(val playerSeat: Wind, val currentSeatWind: Wind) : MatchError
    data class InvalidAction(val actionName: String, val message: String) : MatchError
    data class PlayerNotInMatch(val playerSeat: Wind) : MatchError
    object WallExhausted : MatchError
}