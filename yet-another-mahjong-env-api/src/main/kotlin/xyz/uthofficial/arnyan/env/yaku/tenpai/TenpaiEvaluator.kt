package xyz.uthofficial.arnyan.env.yaku.tenpai

interface TenpaiEvaluator<T, U> {
    fun evaluate(hand: T): U
}