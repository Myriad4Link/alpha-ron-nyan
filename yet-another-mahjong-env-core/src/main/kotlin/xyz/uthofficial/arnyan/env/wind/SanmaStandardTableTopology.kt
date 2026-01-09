package xyz.uthofficial.arnyan.env.wind

class SanmaStandardTableTopology(val seatOrder: List<Wind>) : TableTopology {
    init {
        require(seatOrder.isNotEmpty()) { "Seat order cannot be empty" }
        require(seatOrder.distinct().size == seatOrder.size) { "Seat order cannot contain duplicates" }
    }

    override fun getShimocha(current: Wind): Wind {
        val index = seatOrder.indexOf(current)
        if (index == -1) throw IllegalArgumentException("Wind $current is not in the seat cycle")
        return seatOrder[(index + 1) % seatOrder.size]
    }

    override fun getKamicha(current: Wind): Wind {
        val index = seatOrder.indexOf(current)
        if (index == -1) throw IllegalArgumentException("Wind $current is not in the seat cycle")
        return seatOrder[(index - 1 + seatOrder.size) % seatOrder.size]
    }

    override fun getToimen(current: Wind): Wind {
        throw UnsupportedOperationException("There is no Toimen in Sanma.")
    }
}
