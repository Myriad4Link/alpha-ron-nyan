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
        if (rotationOrder.isEmpty()) {
            return Result.Failure(ConfigurationError.SeatOrderConfigurationError.EmptySeatOrder)
        }
        val duplicates = rotationOrder.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            return Result.Failure(ConfigurationError.SeatOrderConfigurationError.DuplicateSeats(duplicates.toSet()))
        }
        return Result.Success(SanmaStandardTableTopology(rotationOrder))
    }
}