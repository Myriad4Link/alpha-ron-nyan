package xyz.uthofficial.arnyan.env.error

sealed interface ExtractionError : ArnyanError {
    object ExtractionError : xyz.uthofficial.arnyan.env.error.ExtractionError
}