package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result

class RoundWindRotationConfiguration {
    private val windRounds: MutableMap<Wind, Int> = mutableMapOf()

    infix fun Wind.rounds(count: Int): RoundWindRotationConfiguration {
        windRounds[this] = count
        return this@RoundWindRotationConfiguration
    }

    operator fun Wind.times(count: Int): RoundWindRotationConfiguration {
        return this rounds count
    }

    operator fun RoundWindRotationConfiguration.plus(pair: Pair<Wind, Int>): RoundWindRotationConfiguration {
        windRounds[pair.first] = pair.second
        return this@RoundWindRotationConfiguration
    }

    infix fun Wind.to(count: Int): Pair<Wind, Int> = Pair(this, count)

    fun build(): Result<RoundWindCycle, ConfigurationError> {
        return StandardRoundWindCycle.fromMap(windRounds)
    }
}