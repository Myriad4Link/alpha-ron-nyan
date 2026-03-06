package xyz.uthofficial.arnyan.simplified.evolution

import xyz.uthofficial.arnyan.simplified.agent.Playstyle
import java.nio.file.Path
import java.nio.file.Paths

data class EvolutionConfig(
    val lambda: Int = 10,
    val mutationSigma: Float = 0.05f,
    val gamesPerEvaluation: Int = 10,
    val maxGenerations: Int = 100,
    val playstyle: Playstyle = Playstyle.AGGRESSIVE,
    val checkpointInterval: Int = 10,
    val checkpointDir: Path = Paths.get("checkpoints", playstyle.name.lowercase()),
    val parallelMatches: Int = 4,
    val initialMutationSigma: Float = 0.1f,
    val minMutationSigma: Float = 0.01f,
    val sigmaDecayRate: Float = 0.99f
)
