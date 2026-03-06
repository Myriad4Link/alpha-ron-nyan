package xyz.uthofficial.arnyan.env.ruleset.base

import xyz.uthofficial.arnyan.env.scoring.ScoringCalculator

interface ScoringRule {
    fun build(): ScoringCalculator
}