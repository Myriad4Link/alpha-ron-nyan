package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result

interface RoundWindCycle {
    val totalRounds: Int
    val startRoundRotationStatus: RoundRotationStatus

    fun nextRound(current: RoundRotationStatus): Result<RoundRotationStatus, ConfigurationError>
    fun nextHonba(current: RoundRotationStatus): RoundRotationStatus = current.copy(honba = current.honba + 1)
}