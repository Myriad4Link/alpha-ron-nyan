package xyz.uthofficial.arnyan.env.tile

data class TileComposition(val composition: Map<TileType, List<Int>> = emptyMap()) {
    operator fun plus(other: TileComposition): TileComposition {
        val newComposition = (this.composition.asSequence() + other.composition.asSequence())
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.flatten() }
        return TileComposition(newComposition)
    }
}
