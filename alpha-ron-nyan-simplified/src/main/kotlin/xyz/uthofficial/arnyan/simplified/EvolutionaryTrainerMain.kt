package xyz.uthofficial.arnyan.simplified

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import xyz.uthofficial.arnyan.simplified.agent.Playstyle
import xyz.uthofficial.arnyan.simplified.evolution.EvolutionConfig
import xyz.uthofficial.arnyan.simplified.evolution.EvolutionaryTrainer
import xyz.uthofficial.arnyan.simplified.util.RealTimeGrapher
import xyz.uthofficial.arnyan.simplified.util.TrainingVisualizer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("EvolutionaryTrainerMain")

    val quickTest = args.contains("--quickTest") || System.getProperty("quickTest", "false").toBoolean()

    if (quickTest) {
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("QUICK TEST MODE - 1 generation, 1 game")
        logger.info("═══════════════════════════════════════════════════════════")
    }

    val resumeAggressive = args.contains("--resume-aggressive")
    val resumeConservative = args.contains("--resume-conservative")
    val maxGensArg = args.find { it.startsWith("--max-generations=") }?.substringAfter("=")?.toIntOrNull()
    val maxGenerations = maxGensArg ?: if (quickTest) 1 else 42000

    logger.info("Starting Evolutionary Mahjong Agent Training")
    logger.info("Training two agents: AGGRESSIVE and CONSERVATIVE")
    logger.info("Max generations: $maxGenerations")
    logger.info("Resume aggressive: $resumeAggressive")
    logger.info("Resume conservative: $resumeConservative")
    logger.info("")

    val aggressiveConfig = EvolutionConfig(
        lambda = if (quickTest) 2 else 10,
        mutationSigma = 0.05f,
        gamesPerEvaluation = if (quickTest) 1 else 10,
        maxGenerations = maxGenerations,
        playstyle = Playstyle.AGGRESSIVE,
        checkpointInterval = if (quickTest) 1 else 5,
        checkpointDir = Paths.get("checkpoints", "aggressive"),
        parallelMatches = 2,
        initialMutationSigma = 0.1f,
        minMutationSigma = 0.005f,
        sigmaDecayRate = 0.9995f
    )

    val conservativeConfig = EvolutionConfig(
        lambda = if (quickTest) 2 else 10,
        mutationSigma = 0.05f,
        gamesPerEvaluation = if (quickTest) 1 else 10,
        maxGenerations = maxGenerations,
        playstyle = Playstyle.CONSERVATIVE,
        checkpointInterval = if (quickTest) 1 else 5,
        checkpointDir = Paths.get("checkpoints", "conservative"),
        parallelMatches = 2,
        initialMutationSigma = 0.1f,
        minMutationSigma = 0.005f,
        sigmaDecayRate = 0.9995f
    )

    val visualizer = TrainingVisualizer()
    val aggressiveGrapher = RealTimeGrapher()
    val conservativeGrapher = RealTimeGrapher()

    val aggressiveResumePath = if (resumeAggressive) findLatestCheckpoint(aggressiveConfig.checkpointDir) else null
    val conservativeResumePath = if (resumeConservative) findLatestCheckpoint(conservativeConfig.checkpointDir) else null

    if (resumeAggressive && aggressiveResumePath != null) {
        logger.info("Will resume AGGRESSIVE from: $aggressiveResumePath")
    }
    if (resumeConservative && conservativeResumePath != null) {
        logger.info("Will resume CONSERVATIVE from: $conservativeResumePath")
    }

    runBlocking {
        val aggressiveJob = async(Dispatchers.IO) {
            logger.info("═══════════════════════════════════════════════════════════")
            logger.info("Starting AGGRESSIVE agent training")
            logger.info("═══════════════════════════════════════════════════════════")

            visualizer.printTrainingHeader(aggressiveConfig)

            val trainer = EvolutionaryTrainer(aggressiveConfig, aggressiveGrapher, aggressiveResumePath)
            trainer.train()

            logger.info("AGGRESSIVE training complete!")
            trainer.getParent()
        }

        val conservativeJob = async(Dispatchers.IO) {
            logger.info("═══════════════════════════════════════════════════════════")
            logger.info("Starting CONSERVATIVE agent training")
            logger.info("═══════════════════════════════════════════════════════════")

            visualizer.printTrainingHeader(conservativeConfig)

            val trainer = EvolutionaryTrainer(conservativeConfig, conservativeGrapher, conservativeResumePath)
            trainer.train()

            logger.info("CONSERVATIVE training complete!")
            trainer.getParent()
        }

        val aggressiveResult = aggressiveJob.await()
        val conservativeResult = conservativeJob.await()

        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("Training Complete!")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("AGGRESSIVE agent saved to: checkpoints/aggressive/")
        logger.info("CONSERVATIVE agent saved to: checkpoints/conservative/")
        logger.info("")
        logger.info("To load and use an agent:")
        logger.info("  val agent = AgentWeightSerializer.load(")
        logger.info("    Paths.get(\"checkpoints/aggressive/checkpoint_gen_100.bin\"),")
        logger.info("    SanmaPolicyNetwork()")
        logger.info("  )")
        logger.info("")
        
        Thread.sleep(2000)
        
        aggressiveGrapher.close()
        conservativeGrapher.close()
    }
}

private fun findLatestCheckpoint(dir: Path): Path? {
    if (!Files.exists(dir)) {
        return null
    }
    
    return Files.list(dir)
        .filter { it.fileName.toString().startsWith("checkpoint_gen_") }
        .filter { it.fileName.toString().endsWith(".bin") && !it.fileName.toString().contains(".weights.") }
        .toList()
        .maxByOrNull { path -> 
            path.fileName.toString()
                .substringAfter("checkpoint_gen_")
                .substringBefore(".bin")
                .toIntOrNull() ?: 0 
        }
}
