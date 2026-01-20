package xyz.uthofficial.arnyan.env.error

sealed interface ConfigurationError : ArnyanError {
    data class InvalidConfiguration(val message: String, val cause: Throwable? = null) : ConfigurationError
}
