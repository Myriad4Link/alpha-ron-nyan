package xyz.uthofficial.arnyan.env.result

import xyz.uthofficial.arnyan.env.error.ArnyanError

sealed class Result<out T, out E : ArnyanError> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
    data class Failure<out E : ArnyanError>(val error: E) : Result<Nothing, E>()

    fun onSuccess(action: (T) -> Unit): Result<T, E> {
        if (this is Success) action(value)
        return this
    }

    fun onFailure(action: (E) -> Unit): Result<T, E> {
        if (this is Failure) action(error)
        return this
    }

    fun getOrThrow(): T {
        return when (this) {
            is Success -> value
            is Failure -> throw RuntimeException("Result failed: $error")
        }
    }
}
