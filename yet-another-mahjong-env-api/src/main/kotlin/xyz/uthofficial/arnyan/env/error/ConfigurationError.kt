package xyz.uthofficial.arnyan.env.error

import xyz.uthofficial.arnyan.env.wind.Wind

sealed interface ConfigurationError : ArnyanError {
    sealed interface RoundWindConfigurationError : ConfigurationError {
        data class WindNotInRoundCycle(val wind: Wind) : RoundWindConfigurationError
        data class RoundNumberOutOfRange(val wind: Wind, val number: Int, val validRange: IntRange) :
            RoundWindConfigurationError

        data class NoNextRoundBeyondTotal(val totalRounds: Int) : RoundWindConfigurationError
        data object WindRoundsEmpty : RoundWindConfigurationError
        data class RoundCountNotPositive(val invalidCounts: Map<Wind, Int>) : RoundWindConfigurationError
    }

    sealed interface SeatOrderConfigurationError : ConfigurationError {
        data object EmptySeatOrder : SeatOrderConfigurationError
        data class DuplicateSeats(val duplicates: Set<Wind>) : SeatOrderConfigurationError
    }

    sealed interface TileSetConfigurationError : ConfigurationError {
        data class InvalidTileSetConfiguration(val message: String) : TileSetConfigurationError
    }

    sealed interface GenericConfigurationError : ConfigurationError {
        data class InvalidConfiguration(val message: String, val cause: Throwable? = null) : GenericConfigurationError
    }
}
