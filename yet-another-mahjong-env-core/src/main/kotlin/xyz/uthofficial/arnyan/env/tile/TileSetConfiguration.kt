package xyz.uthofficial.arnyan.env.tile

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding

class TileSetConfiguration {
    private var composition: TileComposition = TileComposition()
    var akaDoraConfigurationBuilder: AkaDoraConfigurationBuilder? = null
    var dealAmount = 13

    fun setGroup(block: () -> TileComposition): TileSetConfiguration {
        composition += block()
        return this
    }

    fun setStandardDealAmount(amount: Int): TileSetConfiguration {
        this.dealAmount = amount
        return this
    }

    infix fun repeatFor(amount: Int): TileSetConfiguration {
        composition = TileComposition(composition.composition.mapValues { (_, ints) ->
            val originTiles = ints.toList()
            val newInts = ints.toMutableList()
            repeat(amount - 1) { newInts.addAll(originTiles) }
            newInts
        })
        return this
    }

    fun build(): Result<StandardTileWall, ConfigurationError> = binding {
        binding({ ConfigurationError.GenericConfigurationError.InvalidConfiguration("Failed to build TileSet", it) }) {
            val tileWall = StandardTileWall(standardDealAmount = dealAmount)
            composition.composition.forEach { (type, values) ->
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

    infix fun whereEvery(tileTypesBlock: () -> Iterable<TileType>): AkaDoraConfigurationBuilder =
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
}