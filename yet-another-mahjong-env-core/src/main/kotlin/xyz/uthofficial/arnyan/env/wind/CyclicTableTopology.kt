package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.TopologyError
import xyz.uthofficial.arnyan.env.result.Result

class CyclicTableTopology(override val seats: List<Wind>) : TableTopology {
    override val firstSeatWind: Wind
        get() = seats.first()

    override fun getShimocha(current: Wind): Result<Wind, TopologyError> {
        return when (val index = seats.indexOf(current)) {
            -1 -> Result.Failure(TopologyError.WindNotInCycle(current))
            else -> Result.Success(seats[(index + 1) % seats.size])
        }
    }

    override fun getKamicha(current: Wind): Result<Wind, TopologyError> {
        return when (val index = seats.indexOf(current)) {
            -1 -> Result.Failure(TopologyError.WindNotInCycle(current))
            else -> Result.Success(seats[(index - 1 + seats.size) % seats.size])
        }
    }

    override fun getToimen(current: Wind): Result<Wind, TopologyError> {
        return when (val index = seats.indexOf(current)) {
            -1 -> Result.Failure(TopologyError.WindNotInCycle(current))
            else -> {
                if (seats.size == 4) {
                    Result.Success(seats[(index + 2) % seats.size])
                } else {
                    Result.Failure(TopologyError.NoToimenAvailable)
                }
            }
        }
    }
}