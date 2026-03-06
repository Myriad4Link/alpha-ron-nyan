package xyz.uthofficial.arnyan.simplified.util

import org.slf4j.LoggerFactory
import xyz.uthofficial.arnyan.simplified.evolution.FitnessDetails

class TrainingVisualizer {

    private val logger = LoggerFactory.getLogger(TrainingVisualizer::class.java)

    companion object {
        private const val CHART_WIDTH = 60
        private const val CHART_HEIGHT = 15
    }

    fun printFitnessChart(records: List<GenerationRecord>) {
        if (records.isEmpty()) {
            logger.info("No data to visualize")
            return
        }

        val bestFitnessValues = records.map { it.bestFitness }
        val avgFitnessValues = records.map { it.avgFitness }

        val minFitness = minOf(bestFitnessValues.minOrNull() ?: 0f, avgFitnessValues.minOrNull() ?: 0f)
        val maxFitness = maxOf(bestFitnessValues.maxOrNull() ?: 0f, avgFitnessValues.maxOrNull() ?: 0f)
        val fitnessRange = maxFitness - minFitness

        logger.info("")
        logger.info("╔════════════════════════════════════════════════════════════════════╗")
        logger.info("║                    Training Progress (Fitness)                     ║")
        logger.info("╠════════════════════════════════════════════════════════════════════╣")

        for (row in CHART_HEIGHT downTo 1) {
            val threshold = minFitness + (fitnessRange * row / CHART_HEIGHT)
            val label = "%7.2f".format(threshold)

            val line = buildString {
                append("║ $label │")

                for (col in 0 until CHART_WIDTH) {
                    val recordIdx = (col * records.size / CHART_WIDTH).coerceIn(0, records.size - 1)
                    val record = records[recordIdx]

                    val bestNormalized = (record.bestFitness - minFitness) / fitnessRange
                    val avgNormalized = (record.avgFitness - minFitness) / fitnessRange

                    val bestRow = (bestNormalized * CHART_HEIGHT).toInt().coerceIn(1, CHART_HEIGHT)
                    val avgRow = (avgNormalized * CHART_HEIGHT).toInt().coerceIn(1, CHART_HEIGHT)

                    when {
                        row == bestRow && row == avgRow -> append("█")
                        row == bestRow -> append("▓")
                        row == avgRow -> append("░")
                        else -> append(" ")
                    }
                }

                append(" │")
            }
            logger.info(line)
        }

        logger.info("║        │" + "─".repeat(CHART_WIDTH) + "│")

        val genLabel = "Gen: 1".padEnd(CHART_WIDTH / 2) + "Gen: ${records.size}"
        logger.info("║        │${genLabel}│")
        logger.info("╠════════════════════════════════════════════════════════════════════╣")
        logger.info("║ Legend: ▓ Best Fitness  ░ Avg Fitness                              ║")
        logger.info("╚════════════════════════════════════════════════════════════════════╝")
        logger.info("")
    }

    fun printGenerationSummary(generation: Int, parentFitness: Float, offspringFitness: List<Float>) {
        val bestOffspring = offspringFitness.maxOrNull() ?: 0f
        val avgOffspring = offspringFitness.average().toFloat()
        val minOffspring = offspringFitness.minOrNull() ?: 0f

        val improvement = parentFitness - (offspringFitness.sum() / offspringFitness.size)

        logger.info("")
        logger.info("┌─────────────────────────────────────────────────────────────────────┐")
        logger.info("│ Generation $generation Summary".padEnd(68) + "│")
        logger.info("├─────────────────────────────────────────────────────────────────────┤")
        logger.info("│ Parent Fitness:     ${parentFitness.format(8)}                                       │")
        logger.info("│ Best Offspring:     ${bestOffspring.format(8)}                                       │")
        logger.info("│ Avg Offspring:      ${avgOffspring.format(8)}                                       │")
        logger.info("│ Min Offspring:      ${minOffspring.format(8)}                                       │")
        logger.info("│ Improvement:        ${improvement.format(8)}                                       │")
        logger.info("└─────────────────────────────────────────────────────────────────────┘")
    }

    fun printFitnessDetails(details: FitnessDetails) {
        logger.info("")
        logger.info("Fitness Breakdown (${details.playstyle}): ${details.total}")
        details.breakdown.entries.sortedByDescending { it.value }.forEach { (event, reward) ->
            val bar = "█".repeat((kotlin.math.abs(reward) / 2).toInt().coerceAtLeast(1))
            val sign = if (reward > 0) "+" else ""
            logger.info("  $event: $sign${reward.format(6)} $bar")
        }
        logger.info("")
    }

    fun printTrainingHeader(config: xyz.uthofficial.arnyan.simplified.evolution.EvolutionConfig) {
        logger.info("")
        logger.info("╔════════════════════════════════════════════════════════════════════╗")
        logger.info("║           Evolutionary Mahjong Agent Training                      ║")
        logger.info("╠════════════════════════════════════════════════════════════════════╣")
        logger.info("║ Playstyle:           ${config.playstyle.name.padEnd(43)}║")
        logger.info("║ Population:          1 parent + ${config.lambda} offspring                            ║")
        logger.info("║ Generations:         ${config.maxGenerations}                                              ║")
        logger.info("║ Games/Evaluation:    ${config.gamesPerEvaluation}                                               ║")
        logger.info("║ Mutation Sigma:      ${config.initialMutationSigma} → ${config.minMutationSigma}                              ║")
        logger.info("║ Parallel Matches:    ${config.parallelMatches}                                               ║")
        logger.info("╚════════════════════════════════════════════════════════════════════╝")
        logger.info("")
    }

    private fun Float.format(decimalPlaces: Int): String {
        return "%.${decimalPlaces}f".format(this)
    }
}
