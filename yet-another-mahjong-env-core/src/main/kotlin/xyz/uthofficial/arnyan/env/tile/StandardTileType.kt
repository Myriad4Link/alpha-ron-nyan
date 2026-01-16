package xyz.uthofficial.arnyan.env.tile

// ORDINAL MATTERS HERE!
// The ordinal number will be used in internal yaku-calculation logic to avoid boxing and GC overhead.
enum class StandardTileType(override val intRange: IntRange = 1..9) : TileType {
    MAN,
    SOU,
    PIN,
    // 东 -> 南 -> 西 -> 北
    WIND(1..4),
    // 中 -> 发 -> 白
    DRAGON(1..3)
}
