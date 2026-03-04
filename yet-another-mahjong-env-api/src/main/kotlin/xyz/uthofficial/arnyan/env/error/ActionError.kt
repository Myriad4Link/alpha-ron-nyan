package xyz.uthofficial.arnyan.env.error

import xyz.uthofficial.arnyan.env.result.Result

sealed interface ActionError : ArnyanError {
    data class Match(val error: MatchError) : ActionError
    data class Wall(val error: WallError) : ActionError
    data class Topology(val error: TopologyError) : ActionError
    data class Configuration(val error: ConfigurationError) : ActionError
    data class Generic(val error: ArnyanError) : ActionError
}

fun MatchError.wrapActionError(): ActionError = ActionError.Match(this)
fun WallError.wrapActionError(): ActionError = ActionError.Wall(this)
fun TopologyError.wrapActionError(): ActionError = ActionError.Topology(this)
fun ConfigurationError.wrapActionError(): ActionError = ActionError.Configuration(this)

fun <T, E : ArnyanError> Result<T, E>.wrapActionError(): Result<T, ActionError> =
    mapError { error ->
        when (error) {
            is MatchError -> ActionError.Match(error)
            is WallError -> ActionError.Wall(error)
            is TopologyError -> ActionError.Topology(error)
            is ConfigurationError -> ActionError.Configuration(error)
            else -> ActionError.Generic(error)
        }
    }