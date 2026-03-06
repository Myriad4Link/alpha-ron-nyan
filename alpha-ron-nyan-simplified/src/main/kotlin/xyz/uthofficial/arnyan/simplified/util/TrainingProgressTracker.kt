package xyz.uthofficial.arnyan.simplified.util

import java.nio.file.Path
import java.nio.file.Files

data class GenerationRecord(
    val generation: Int,
    val bestFitness: Float,
    val avgFitness: Float,
    val mutationSigma: Float
)

class TrainingProgressTracker(private val csvPath: Path) {

    private val records = mutableListOf<GenerationRecord>()

    fun record(generation: Int, bestFitness: Float, avgFitness: Float, sigma: Float) {
        records.add(GenerationRecord(generation, bestFitness, avgFitness, sigma))
    }

    fun exportCSV() {
        csvPath.parent?.let { Files.createDirectories(it) }
        csvPath.toFile().writeText(buildString {
            appendLine("generation,best_fitness,avg_fitness,mutation_sigma")
            records.forEach { record ->
                appendLine("${record.generation},${record.bestFitness},${record.avgFitness},${record.mutationSigma}")
            }
        })
    }

    fun getRecords(): List<GenerationRecord> = records.toList()

    fun getBestFitness(): Float = records.maxByOrNull { it.bestFitness }?.bestFitness ?: 0f

    fun getLatestFitness(): Float = records.lastOrNull()?.bestFitness ?: 0f

    fun getFitnessImprovement(): Float {
        if (records.size < 2) return 0f
        return records.last().bestFitness - records.first().bestFitness
    }
}
