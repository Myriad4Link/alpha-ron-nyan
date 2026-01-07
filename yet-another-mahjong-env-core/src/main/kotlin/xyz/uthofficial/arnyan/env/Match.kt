package xyz.uthofficial.arnyan.env

interface Match {
    val isEnded: Boolean
    fun initialize() : Match
}