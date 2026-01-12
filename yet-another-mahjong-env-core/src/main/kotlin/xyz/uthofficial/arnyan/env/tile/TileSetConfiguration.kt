package xyz.uthofficial.arnyan.env.tile

class TileSetConfiguration {
    private val buildBlocks: MutableList<TileSetConfiguration.() -> Unit> = mutableListOf()
    val composition: MutableMap<TileType, MutableList<Int>> = mutableMapOf()
    var akaDoraConfigurationBuilder: AkaDoraConfigurationBuilder? = null

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

    fun build(): Result<TileWall> {
        return runCatching {
            buildBlocks.forEach { this.it() }

            val tileWall = TileWall()
            // What `composition` looks like:
            // {
            //    "WAN": [1, 1, 1, 1, 9, 9, 9, 9],
            //    "SOU": [1, 2, 3, ..., 9, 1, 2, 3, ..., 9, ..., 9],
            //    ...
            // }
            composition.forEach { (type, values) ->
                val akaConfig = akaDoraConfigurationBuilder?.akaDoraConfiguration?.get(type)
                var akaQuota = akaConfig?.first ?: 0
                val akaValueTarget = akaConfig?.second ?: -1

                val tiles = values.map {
                    val isAka = (it == akaValueTarget && akaQuota > 0)
                    if (isAka) akaQuota--
                    Tile(type, it, isAka)
                }

                tileWall.addAll(tiles)
            }
            tileWall
        }
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

    infix fun whereEvery(tileTypesBlock: TileSetConfiguration.() -> Iterable<TileType>): AkaDoraConfigurationBuilder =
        AkaDoraConfigurationBuilder(tileTypesBlock(), this)

    class AkaDoraConfigurationBuilder(val tileTypes: Iterable<TileType>, val parent: TileSetConfiguration) {
        var amount: Int = 0
        var on: Int? = null
        val akaDoraConfiguration: MutableMap<TileType, Pair<Int, Int>> = mutableMapOf()

        infix fun has(amount: Int): AkaDoraConfigurationBuilder {
            this.amount = amount
            return this
        }

        infix fun redDoraOn(on: Int): TileSetConfiguration {
            this.on = on
            tileTypes.forEach { akaDoraConfiguration += it to (amount to on) }
            parent.akaDoraConfigurationBuilder = this
            return parent
        }
    }

    infix fun TileType.and(operand: TileType): MutableList<TileType> = mutableListOf(this, operand)
    infix fun MutableList<TileType>.and(operand: TileType): MutableList<TileType> {
        this.add(operand)
        return this
    }

    fun allOf(tileType: TileType) = this.allOf(listOf(tileType))
    fun allOf(tileTypes: Iterable<TileType>) = tileTypes.forEach { it.intRange of it }
}