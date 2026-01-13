package xyz.uthofficial.arnyan.env.error

sealed interface WallError : ArnyanError {
    data class NotEnoughTiles(val requested: Int, val available: Int) : WallError
}
