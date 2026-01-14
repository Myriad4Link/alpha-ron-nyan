package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.wind.Wind

data class StepResult(val observation: MatchObservation, val nextWind: Wind, val isOver: Boolean)