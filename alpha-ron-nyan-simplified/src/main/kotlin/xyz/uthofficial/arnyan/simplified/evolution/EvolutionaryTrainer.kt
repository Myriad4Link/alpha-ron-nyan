package xyz.uthofficial.arnyan.simplified.evolution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import xyz.uthofficial.arnyan.simplified.agent.EvolutionaryAgent
import xyz.uthofficial.arnyan.simplified.network.SanmaPolicyNetwork
import xyz.uthofficial.arnyan.simplified.util.AgentWeightSerializer
import xyz.uthofficial.arnyan.simplified.util.CheckpointMetadata
import xyz.uthofficial.arnyan.simplified.util.TrainingProgressTracker
import java.nio.file.Files
import kotlin.math.sqrt

class EvolutionaryTrainer(private val config: EvolutionConfig) {

    private val logger = LoggerFactory.getLogger(EvolutionaryTrainer::class.java)
    private val matchSimulator = MatchSimulator()
    private val fitnessCalculator = FitnessCalculator(config.playstyle)
    private val progressTracker = TrainingProgressTracker(
        config.checkpointDir.resolve("training_progress.csv")
    )

    private lateinit var parent: EvolutionaryAgent
    private var currentSigma = config.initialMutationSigma
    private var bestFitnessEver = -Float.MAX_VALUE

    fun train() {
        logger.info("Starting evolutionary training with config: $config")
        logger.info("Playstyle: ${config.playstyle}")
        logger.info("Population: 1 parent + ${config.lambda} offspring")
        logger.info("Mutation sigma: ${config.initialMutationSigma} -> ${config.minMutationSigma}")

        Files.createDirectories(config.checkpointDir)

        parent = initializeParent()

        logger.info("Parent initialized with ${parent.network.countParameters()} parameters")

        runBlocking {
            for (generation in 1..config.maxGenerations) {
                logger.info("=== Generation $generation ===")

                val parentFitness = evaluateAgent(parent)
                logger.info("Parent fitness: $parentFitness")

                if (parentFitness > bestFitnessEver) {
                    bestFitnessEver = parentFitness
                    logger.info("New best fitness ever: $bestFitnessEver")
                }

                val offspring = generateOffspring(parent, currentSigma)
                logger.info("Generated ${offspring.size} offspring")

                val offspringFitness = evaluateOffspring(offspring)
                logger.info("Offspring fitness: min=${offspringFitness.minOrNull()}, max=${offspringFitness.maxOrNull()}, avg=${offspringFitness.average()}")

                val bestOffspringIdx = offspringFitness.indices.maxByOrNull { offspringFitness[it] } ?: 0
                val bestOffspringFitness = offspringFitness[bestOffspringIdx]

                if (bestOffspringFitness > parentFitness) {
                    logger.info("Offspring ${bestOffspringIdx} wins with fitness $bestOffspringFitness > $parentFitness")
                    parent = offspring[bestOffspringIdx]
                } else {
                    logger.info("Parent survives ($parentFitness >= $bestOffspringFitness)")
                }

                progressTracker.record(generation, parentFitness, offspringFitness.average().toFloat(), currentSigma)

                if (generation % config.checkpointInterval == 0) {
                    saveCheckpoint(generation, parentFitness)
                }

                currentSigma = (currentSigma * config.sigmaDecayRate).coerceAtLeast(config.minMutationSigma)

                logger.info("")
            }
        }

        saveCheckpoint(config.maxGenerations, bestFitnessEver)
        progressTracker.exportCSV()

        logger.info("Training complete! Best fitness: $bestFitnessEver")
        logger.info("Checkpoint saved to: ${config.checkpointDir}")
    }

    private fun initializeParent(): EvolutionaryAgent {
        val network = SanmaPolicyNetwork()
        val initialWeights = FloatArray(network.countParameters()) {
            kotlin.random.Random.nextFloat() * 0.1f - 0.05f
        }
        network.setWeights(initialWeights)

        return EvolutionaryAgent(
            network = network,
            playstyle = config.playstyle,
            temperature = 1.0f
        )
    }

    private suspend fun evaluateAgent(agent: EvolutionaryAgent): Float {
        val opponents = matchSimulator.createRandomOpponents(2)
        return matchSimulator.evaluateAgent(agent, opponents, config.gamesPerEvaluation, fitnessCalculator)
    }

    private fun generateOffspring(parent: EvolutionaryAgent, sigma: Float): List<EvolutionaryAgent> {
        return List(config.lambda) {
            mutate(parent, sigma)
        }
    }

    private fun mutate(agent: EvolutionaryAgent, sigma: Float): EvolutionaryAgent {
        val weights = agent.network.getWeights()
        val mutated = weights.map { weight ->
            weight + nextGaussian() * sigma
        }.toFloatArray()

        return agent.cloneWithWeights(mutated)
    }

    private fun nextGaussian(): Float {
        val u1 = kotlin.random.Random.nextFloat()
        val u2 = kotlin.random.Random.nextFloat()
        return (sqrt(-2.0 * kotlin.math.ln(u1.coerceAtLeast(1e-10f))) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)).toFloat()
    }

    private suspend fun evaluateOffspring(offspring: List<EvolutionaryAgent>): List<Float> {
        return kotlinx.coroutines.coroutineScope {
            offspring.map { agent ->
                async(Dispatchers.IO.limitedParallelism(config.parallelMatches)) {
                    val opponents = matchSimulator.createRandomOpponents(2)
                    matchSimulator.evaluateAgent(agent, opponents, config.gamesPerEvaluation, fitnessCalculator)
                }
            }.awaitAll()
        }
    }

    private fun saveCheckpoint(generation: Int, fitness: Float) {
        val checkpointPath = config.checkpointDir.resolve("checkpoint_gen_${generation}.bin")
        val metadata = CheckpointMetadata(
            generation = generation,
            fitness = fitness,
            playstyle = config.playstyle,
            timestamp = System.currentTimeMillis(),
            mutationSigma = currentSigma,
            bestFitnessEver = bestFitnessEver
        )

        AgentWeightSerializer.save(parent, checkpointPath, metadata)
        logger.info("Checkpoint saved: $checkpointPath")
    }

    fun getParent(): EvolutionaryAgent = parent
    fun getBestFitnessEver(): Float = bestFitnessEver
}
