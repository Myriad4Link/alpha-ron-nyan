package xyz.uthofficial.arnyan.env.yaku.resolver

interface TileResolver<T, U> {
    fun resolve(hand: T): U
}