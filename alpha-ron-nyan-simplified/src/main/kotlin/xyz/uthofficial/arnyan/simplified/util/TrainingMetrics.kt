package xyz.uthofficial.arnyan.simplified.util

data class OffspringEvaluation(
    val generation: Int,
    val offspringIndex: Int,
    val fitness: Float,
    val gamesPlayed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class GenerationMetrics(
    val generation: Int,
    val parentFitness: Float,
    val offspringFitnesses: List<Float>,
    val mutationSigma: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    val bestOffspringFitness: Float get() = offspringFitnesses.maxOrNull() ?: 0f
    val avgOffspringFitness: Float get() = offspringFitnesses.average().toFloat()
    val minOffspringFitness: Float get() = offspringFitnesses.minOrNull() ?: 0f
    val fitnessImprovement: Float get() = bestOffspringFitness - parentFitness
}

data class WeightStatistics(
    val generation: Int,
    val mean: Float,
    val stdDev: Float,
    val min: Float,
    val max: Float,
    val histogram: IntArray,
    val binEdges: FloatArray
)

sealed class MetricUpdate {
    data class OffspringEvaluated(val evaluation: OffspringEvaluation) : MetricUpdate()
    data class GenerationComplete(val metrics: GenerationMetrics) : MetricUpdate()
    data class WeightsUpdated(val stats: WeightStatistics) : MetricUpdate()
    object TrainingComplete : MetricUpdate()
}
