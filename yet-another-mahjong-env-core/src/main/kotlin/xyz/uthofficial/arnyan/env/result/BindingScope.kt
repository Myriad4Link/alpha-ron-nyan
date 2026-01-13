package xyz.uthofficial.arnyan.env.result

import xyz.uthofficial.arnyan.env.error.ArnyanError

class BindingScope<E : ArnyanError> {
    var error: E? = null
        internal set

    fun <T> Result<T, E>.bind(): T {
        return when (this) {
            is Result.Success -> value
            is Result.Failure -> {
                this@BindingScope.error = this.error
                throw BindException()
            }
        }
    }

    fun <T> binding(converter: (Throwable) -> E, block: BindingScope<E>.() -> T): T {
        try {
            return this.block()
        } catch (e: BindException) {
            throw e
        } catch (e: Throwable) {
            this.error = converter(e)
            throw BindException()
        }
    }
}

fun <T, E : ArnyanError> binding(block: BindingScope<E>.() -> T): Result<T, E> {
    val scope = BindingScope<E>()
    return try {
        Result.Success(scope.block())
    } catch (_: BindException) {
        val error = scope.error ?: throw IllegalStateException("BindException thrown but no error captured")
        Result.Failure(error)
    }
}
