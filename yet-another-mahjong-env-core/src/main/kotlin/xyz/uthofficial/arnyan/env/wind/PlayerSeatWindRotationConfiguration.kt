package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result

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

    fun build(): Result<TableTopology, ConfigurationError> {
        return when {
            rotationOrder.isEmpty() -> Result.Failure(ConfigurationError.InvalidConfiguration("Seat order cannot be empty"))

            rotationOrder.distinct().size != rotationOrder.size ->
                Result.Failure(ConfigurationError.InvalidConfiguration("Seat order cannot contain duplicates"))

            else -> Result.Success(SanmaStandardTableTopology(rotationOrder))
        }
    }
}