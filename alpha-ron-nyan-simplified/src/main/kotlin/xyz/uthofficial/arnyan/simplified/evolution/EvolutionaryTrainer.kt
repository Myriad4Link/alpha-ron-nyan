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
import xyz.uthofficial.arnyan.simplified.util.GenerationMetrics
import xyz.uthofficial.arnyan.simplified.util.MetricUpdate
import xyz.uthofficial.arnyan.simplified.util.OffspringEvaluation
import xyz.uthofficial.arnyan.simplified.util.RealTimeGrapher
import xyz.uthofficial.arnyan.simplified.util.TrainingProgressTracker
import xyz.uthofficial.arnyan.simplified.util.WeightStatistics
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.sqrt

class EvolutionaryTrainer(
    private val config: EvolutionConfig,
    private val grapher: RealTimeGrapher? = null,
    private val resumeFrom: Path? = null
) {

    private val logger = LoggerFactory.getLogger(EvolutionaryTrainer::class.java)
    private val matchSimulator = MatchSimulator()
    private val fitnessCalculator = FitnessCalculator(config.playstyle)
    private val progressTracker = TrainingProgressTracker(
        config.checkpointDir.resolve("training_progress.csv")
    )

    private lateinit var parent: EvolutionaryAgent
    private var currentSigma = config.initialMutationSigma
    private var bestFitnessEver = -Float.MAX_VALUE
    private var startGeneration = 0

    fun train() {
        logger.info("Starting evolutionary training with config: $config")
        logger.info("Playstyle: ${config.playstyle}")
        logger.info("Population: 1 parent + ${config.lambda} offspring")
        logger.info("Mutation sigma: ${config.initialMutationSigma} -> ${config.minMutationSigma}")

        Files.createDirectories(config.checkpointDir)

        val initResult = initializeParent()
        parent = initResult.first
        startGeneration = initResult.second

        logger.info("Parent initialized with ${parent.network.countParameters()} parameters")
        if (startGeneration > 0) {
            logger.info("Resumed from generation $startGeneration, sigma=$currentSigma, bestFitness=$bestFitnessEver")
        }
        
        grapher?.update(MetricUpdate.WeightsUpdated(calculateWeightStats(startGeneration, parent)))

        runBlocking {
            for (generation in (startGeneration + 1)..config.maxGenerations) {
                logger.info("=== Generation $generation ===")

                val parentFitness = evaluateAgent(parent)
                logger.info("Parent fitness: $parentFitness")

                val offspring = generateOffspring(parent, currentSigma)
                logger.info("Generated ${offspring.size} offspring")

                val offspringFitness = evaluateOffspringWithCallbacks(offspring, generation)

                val bestOffspringIdx = offspringFitness.indices.maxByOrNull { offspringFitness[it] } ?: 0
                val bestOffspringFitness = offspringFitness[bestOffspringIdx]

                if (bestOffspringFitness > parentFitness) {
                    logger.info("Offspring ${bestOffspringIdx} wins with fitness $bestOffspringFitness > $parentFitness")
                    parent = offspring[bestOffspringIdx]
                } else {
                    logger.info("Parent survives ($parentFitness >= $bestOffspringFitness)")
                }

                if (parentFitness > bestFitnessEver) {
                    bestFitnessEver = parentFitness
                    logger.info("New best fitness ever: $bestFitnessEver")
                    saveBestEverCheckpoint(generation, bestFitnessEver)
                }

                progressTracker.record(generation, parentFitness, offspringFitness.average().toFloat(), currentSigma)
                
                grapher?.update(
                    MetricUpdate.GenerationComplete(
                        GenerationMetrics(
                            generation = generation,
                            parentFitness = parentFitness,
                            offspringFitnesses = offspringFitness,
                            mutationSigma = currentSigma
                        )
                    )
                )
                
                grapher?.update(MetricUpdate.WeightsUpdated(calculateWeightStats(generation, parent)))

                if (generation % config.checkpointInterval == 0) {
                    saveCheckpoint(generation, parentFitness)
                }

                currentSigma = (currentSigma * config.sigmaDecayRate).coerceAtLeast(config.minMutationSigma)

                logger.info("")
            }
        }

        saveCheckpoint(config.maxGenerations, bestFitnessEver)
        progressTracker.exportCSV()
        
        grapher?.update(MetricUpdate.TrainingComplete)

        logger.info("Training complete! Best fitness: $bestFitnessEver")
        logger.info("Checkpoint saved to: ${config.checkpointDir}")
    }

    private fun calculateWeightStats(generation: Int, agent: EvolutionaryAgent): WeightStatistics {
        val weights = agent.network.getWeights()
        val mean = weights.average().toFloat()
        val variance = weights.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance)
        val min = weights.minOrNull() ?: 0f
        val max = weights.maxOrNull() ?: 0f
        
        val numBins = 20
        val binWidth = (max - min) / numBins
        val histogram = IntArray(numBins)
        
        weights.forEach { weight ->
            val binIndex = ((weight - min) / binWidth).toInt().coerceIn(0, numBins - 1)
            histogram[binIndex]++
        }
        
        val binEdges = FloatArray(numBins + 1) { i ->
            min + (i * binWidth)
        }
        
        return WeightStatistics(
            generation = generation,
            mean = mean,
            stdDev = stdDev,
            min = min,
            max = max,
            histogram = histogram,
            binEdges = binEdges
        )
    }

    private fun initializeParent(): Pair<EvolutionaryAgent, Int> {
        if (resumeFrom != null && Files.exists(resumeFrom)) {
            logger.info("Loading checkpoint from: $resumeFrom")
            val (agent, metadata) = AgentWeightSerializer.load(resumeFrom, SanmaPolicyNetwork())
            currentSigma = metadata.mutationSigma
            bestFitnessEver = metadata.bestFitnessEver
            return agent to metadata.generation
        }

        val network = SanmaPolicyNetwork()
        val initialWeights = FloatArray(network.countParameters()) {
            kotlin.random.Random.nextFloat() * 0.1f - 0.05f
        }
        network.setWeights(initialWeights)

        val agent = EvolutionaryAgent(
            network = network,
            playstyle = config.playstyle,
            temperature = 1.0f
        )
        return agent to 0
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

    private suspend fun evaluateOffspringWithCallbacks(offspring: List<EvolutionaryAgent>, generation: Int): List<Float> {
        return kotlinx.coroutines.coroutineScope {
            offspring.mapIndexed { index, agent ->
                async(Dispatchers.IO.limitedParallelism(config.parallelMatches)) {
                    val opponents = matchSimulator.createRandomOpponents(2)
                    val fitness = matchSimulator.evaluateAgent(agent, opponents, config.gamesPerEvaluation, fitnessCalculator)
                    
                    grapher?.update(
                        MetricUpdate.OffspringEvaluated(
                            OffspringEvaluation(
                                generation = generation,
                                offspringIndex = index,
                                fitness = fitness,
                                gamesPlayed = config.gamesPerEvaluation
                            )
                        )
                    )
                    
                    fitness
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

    private fun saveBestEverCheckpoint(generation: Int, fitness: Float) {
        val bestEverPath = config.checkpointDir.resolve("best_ever.bin")
        val metadata = CheckpointMetadata(
            generation = generation,
            fitness = fitness,
            playstyle = config.playstyle,
            timestamp = System.currentTimeMillis(),
            mutationSigma = currentSigma,
            bestFitnessEver = fitness
        )

        AgentWeightSerializer.save(parent, bestEverPath, metadata)
        logger.info("Best-ever checkpoint saved: $bestEverPath (fitness: $fitness)")
    }

    fun getParent(): EvolutionaryAgent = parent
    fun getBestFitnessEver(): Float = bestFitnessEver
}
