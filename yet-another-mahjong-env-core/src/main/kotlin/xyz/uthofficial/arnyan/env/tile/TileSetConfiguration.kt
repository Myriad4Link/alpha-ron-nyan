package xyz.uthofficial.arnyan.env.tile

class TileSetConfiguration {
    private val buildBlocks: MutableList<TileSetConfiguration.() -> Unit> = mutableListOf()
    val composition: MutableMap<TileType, MutableList<Int>> = mutableMapOf()
    var redDoraConfigurationBuilder: RedDoraConfigurationBuilder? = null

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
            val tilesForType = values.map { Tile(type, it) }
            val redConfig = redDoraConfigurationBuilder?.redDoraConfiguration?.get(type)

            when {
                redConfig != null -> {
                    val (amountToMark, valueOn) = redConfig
                    tilesForType.filter { it.value == valueOn }
                        .take(amountToMark)
                        .forEach { it.isAka = true }
                }
            }

            tilesForType.forEach { tileWall.add(it) }
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

    infix fun whereEvery(tileTypesBlock: TileSetConfiguration.() -> Iterable<TileType>): RedDoraConfigurationBuilder =
        RedDoraConfigurationBuilder(tileTypesBlock(), this)

    class RedDoraConfigurationBuilder(val tileTypes: Iterable<TileType>, val parent: TileSetConfiguration) {
        var amount: Int = 0
        var on: Int? = null
        val redDoraConfiguration: MutableMap<TileType, Pair<Int, Int>> = mutableMapOf()

        infix fun has(amount: Int): RedDoraConfigurationBuilder {
            this.amount = amount
            return this
        }

        infix fun redDoraOn(on: Int): TileSetConfiguration {
            this.on = on
            tileTypes.forEach { redDoraConfiguration += it to (amount to on) }
            parent.redDoraConfigurationBuilder = this
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