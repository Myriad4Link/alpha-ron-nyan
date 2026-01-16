package xyz.uthofficial.arnyan.env.yaku.extractor

data class ExtractionResult(
    val mentsu: Mentsu,
    // Here we use Map<(tile id), (tile value)>.
    // So that we can ignore the boxing and GC overhead for using Tile objects.
    val consumedCounts: Map<Int, Int>
)
