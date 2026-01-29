package xyz.uthofficial.arnyan.env.yaku

interface Yaku {
    val preconditions: List<() -> Boolean>
}