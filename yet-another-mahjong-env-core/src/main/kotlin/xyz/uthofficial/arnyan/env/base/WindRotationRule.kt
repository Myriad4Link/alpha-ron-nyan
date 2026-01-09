package xyz.uthofficial.arnyan.env.base

import xyz.uthofficial.arnyan.env.wind.TableTopology

fun interface WindRotationRule {
    fun build(): TableTopology
}