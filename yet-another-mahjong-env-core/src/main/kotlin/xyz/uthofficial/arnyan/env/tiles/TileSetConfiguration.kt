package xyz.uthofficial.arnyan.env.tiles

class TileSetConfiguration {
    private val buildBlocks: MutableList<TileSetConfiguration.() -> Unit> = mutableListOf()
    val composition: MutableMap<TileType, MutableList<Int>> = mutableMapOf()

    fun setGroup(block: TileSetConfiguration.() -> Unit): TileSetConfiguration {
        buildBlocks.add(block)
        return this
    }

    infix fun repeatFor(amount: Int): TileSetConfiguration {
        buildBlocks.add {
            composition.forEach { (_, ints) ->
                val originTiles = ints.toList()
                repeat(amount - 1) { ints.addAll(originTiles) }
            }
        }

        return this
    }

    fun build(): TileWall {
        buildBlocks.forEach { this.it() }

        val tileWall = TileWall()
        composition.forEach { (type, values) ->
            values.forEach {
                tileWall.add(Tile(type, it))
            }
        }

        return tileWall
    }

    infix fun Iterable<Int>.of(tileType: TileType) {
        this of listOf(tileType)
    }

    infix fun Iterable<Int>.of(tileTypes: Iterable<TileType>) {
        val numbers = this.toList()

        tileTypes.forEach { type ->
            composition.getOrPut(type) { mutableListOf() }.addAll(numbers)
        }
    }

    infix fun MutableList<Int>.and(operand: Int): MutableList<Int> {
        this.add(operand)
        return this
    }

    infix fun TileType.and(operand: TileType): MutableList<TileType> = mutableListOf(this, operand)
    infix fun MutableList<TileType>.and(operand: TileType): MutableList<TileType> {
        this.add(operand)
        return this
    }
}