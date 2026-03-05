package xyz.uthofficial.arnyan.env.scoring

import xyz.uthofficial.arnyan.env.ruleset.base.ScoringRule

object StandardScoringRule : ScoringRule {
    override fun build(): ScoringCalculator {
        return StandardScoringCalculator()
    }
}