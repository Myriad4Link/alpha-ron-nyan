package xyz.uthofficial.arnyan.env.wind

class SanmaStandardTableTopology(override val seats: List<Wind>) : TableTopology {

    override fun getShimocha(current: Wind): Result<Wind> {
        return when (val index = seats.indexOf(current)) {
            -1 -> Result.failure(IllegalArgumentException("Wind $current is not in the seat cycle"))
            else -> Result.success(seats[(index + 1) % seats.size])
        }
    }

    override fun getKamicha(current: Wind): Result<Wind> {
        return when (val index = seats.indexOf(current)) {
            -1 -> Result.failure(IllegalArgumentException("Wind $current is not in the seat cycle"))
            else -> Result.success(seats[(index - 1 + seats.size) % seats.size])
        }
    }

    override fun getToimen(current: Wind): Result<Wind> {
        return Result.failure(UnsupportedOperationException("There is no Toimen in Sanma."))
    }
}
