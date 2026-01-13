package xyz.uthofficial.arnyan.env.base

import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.wind.TableTopology

fun interface WindRotationRule {
    fun build(): Result<TableTopology, ConfigurationError>
}