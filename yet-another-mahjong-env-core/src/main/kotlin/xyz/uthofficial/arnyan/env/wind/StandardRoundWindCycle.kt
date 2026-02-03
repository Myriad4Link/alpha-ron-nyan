package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result

class StandardRoundWindCycle private constructor(
    private val windSequence: List<Pair<Wind, Int>>
) : RoundWindCycle {
    private val winds: List<Wind> = windSequence.flatMap { (place, count) -> List(count) { place } }
    private val windRounds: Map<Wind, Int> = windSequence.toMap()

    override val totalRounds: Int
        get() = windSequence.sumOf { it.second }

    override val startRoundRotationStatus: RoundRotationStatus
        get() = RoundRotationStatus(winds[0], 1, 0)

    override fun nextRound(current: RoundRotationStatus): Result<RoundRotationStatus, ConfigurationError> {
        val windCount = windRounds[current.place] ?: return Result.Failure(
            ConfigurationError.RoundWindConfigurationError.WindNotInRoundCycle(current.place)
        )
        if (current.round !in 1..windCount) {
            return Result.Failure(
                ConfigurationError.RoundWindConfigurationError.RoundNumberOutOfRange(
                    current.place,
                    current.round,
                    1..windCount
                )
            )
        }
        var index = 0
        for ((place, count) in windSequence) {
            if (place == current.place) {
                index += current.round - 1
                break
            }
            index += count
        }
        index += 1
        if (index >= totalRounds) {
            return Result.Failure(
                ConfigurationError.RoundWindConfigurationError.NoNextRoundBeyondTotal(totalRounds)
            )
        }
        var remaining = index
        for ((place, count) in windSequence) {
            if (remaining < count) {
                return Result.Success(RoundRotationStatus(place, remaining + 1, 0))
            }
            remaining -= count
        }
        return Result.Failure(ConfigurationError.GenericConfigurationError.InvalidConfiguration("Internal error computing next round"))
    }

    companion object {
        fun fromMap(windRounds: Map<Wind, Int>): Result<RoundWindCycle, ConfigurationError> {
            val invalidCounts = windRounds.filter { (_, count) -> count <= 0 }
            return when {
                windRounds.isEmpty() -> Result.Failure(ConfigurationError.RoundWindConfigurationError.WindRoundsEmpty)
                invalidCounts.isNotEmpty() -> Result.Failure(
                    ConfigurationError.RoundWindConfigurationError.RoundCountNotPositive(
                        invalidCounts
                    )
                )

                else -> Result.Success(StandardRoundWindCycle(windRounds.toList()))
            }
        }
    }
}