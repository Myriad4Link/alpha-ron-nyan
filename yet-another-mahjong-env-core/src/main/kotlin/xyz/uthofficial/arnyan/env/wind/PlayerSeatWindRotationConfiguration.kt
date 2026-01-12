package xyz.uthofficial.arnyan.env.wind

class PlayerSeatWindRotationConfiguration {
    private val _rotationOrder: MutableList<Wind> = mutableListOf()
    val rotationOrder: List<Wind> get() = _rotationOrder.toList()

    operator fun Wind.minus(wind: Wind): MutableList<Wind> {
        _rotationOrder.add(this)
        _rotationOrder.add(wind)
        return _rotationOrder
    }

    operator fun Iterable<Wind>.minus(wind: Wind): MutableList<Wind> {
        _rotationOrder.add(wind)
        return _rotationOrder
    }

    fun build(): Result<TableTopology> {
        return when {
            rotationOrder.isEmpty() -> Result.failure(IllegalArgumentException("Seat order cannot be empty"))

            rotationOrder.distinct().size != rotationOrder.size ->
                Result.failure(IllegalArgumentException("Seat order cannot contain duplicates"))

            else -> Result.success(SanmaStandardTableTopology(rotationOrder))
        }
    }
}