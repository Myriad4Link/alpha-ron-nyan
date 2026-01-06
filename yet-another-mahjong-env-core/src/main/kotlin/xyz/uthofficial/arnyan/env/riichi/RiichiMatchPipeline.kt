package xyz.uthofficial.arnyan.env.riichi

import xyz.uthofficial.arnyan.env.Match
import xyz.uthofficial.arnyan.env.MatchPipeline

class RiichiMatchPipeline : MatchPipeline {
    private val untilDefault: (Match) -> Boolean = { m -> m.isEnded }

    fun execute(until: (Match) -> Boolean = untilDefault) {

    }
}