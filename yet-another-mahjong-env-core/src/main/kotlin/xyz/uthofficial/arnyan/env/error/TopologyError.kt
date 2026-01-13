package xyz.uthofficial.arnyan.env.error

import xyz.uthofficial.arnyan.env.wind.Wind

sealed interface TopologyError : ArnyanError {
    data class WindNotInCycle(val wind: Wind) : TopologyError
    data object NoToimenAvailable : TopologyError
}
