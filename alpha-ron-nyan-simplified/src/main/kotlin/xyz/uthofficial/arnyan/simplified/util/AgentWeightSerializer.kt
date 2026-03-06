package xyz.uthofficial.arnyan.simplified.util

import xyz.uthofficial.arnyan.simplified.agent.EvolutionaryAgent
import xyz.uthofficial.arnyan.simplified.agent.Playstyle
import xyz.uthofficial.arnyan.simplified.network.SanmaPolicyNetwork
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path

data class CheckpointMetadata(
    val generation: Int,
    val fitness: Float,
    val playstyle: Playstyle,
    val timestamp: Long,
    val mutationSigma: Float = 0.05f,
    val bestFitnessEver: Float = 0f
)

object AgentWeightSerializer {

    fun save(agent: EvolutionaryAgent, path: Path, metadata: CheckpointMetadata) {
        path.parent?.toFile()?.mkdirs()
        DataOutputStream(path.toFile().outputStream()).use { dos ->
            dos.writeInt(CONSTANTS.MAGIC_NUMBER)
            dos.writeInt(CONSTANTS.VERSION)

            dos.writeInt(metadata.generation)
            dos.writeFloat(metadata.fitness)
            dos.writeUTF(metadata.playstyle.name)
            dos.writeLong(metadata.timestamp)
            dos.writeFloat(metadata.mutationSigma)
            dos.writeFloat(metadata.bestFitnessEver)

            agent.network.saveBinary(path.parent.resolve(path.fileName.toString().replace(".bin", ".weights.bin")))
        }
    }

    fun load(path: Path, network: SanmaPolicyNetwork): Pair<EvolutionaryAgent, CheckpointMetadata> {
        DataInputStream(path.toFile().inputStream()).use { dis ->
            val magic = dis.readInt()
            require(magic == CONSTANTS.MAGIC_NUMBER) {
                "Invalid checkpoint file: bad magic number $magic (expected ${CONSTANTS.MAGIC_NUMBER})"
            }

            val version = dis.readInt()
            require(version == CONSTANTS.VERSION) {
                "Unsupported checkpoint version: $version (expected ${CONSTANTS.VERSION})"
            }

            val generation = dis.readInt()
            val fitness = dis.readFloat()
            val playstyleName = dis.readUTF()
            val timestamp = dis.readLong()
            val mutationSigma = dis.readFloat()
            val bestFitnessEver = dis.readFloat()

            val weightsPath = path.parent.resolve(path.fileName.toString().replace(".bin", ".weights.bin"))
            network.loadBinary(weightsPath)

            val metadata = CheckpointMetadata(
                generation = generation,
                fitness = fitness,
                playstyle = Playstyle.valueOf(playstyleName),
                timestamp = timestamp,
                mutationSigma = mutationSigma,
                bestFitnessEver = bestFitnessEver
            )

            val agent = EvolutionaryAgent(
                network = network,
                playstyle = metadata.playstyle,
                temperature = 1.0f
            )

            return agent to metadata
        }
    }

    fun loadMetadataOnly(path: Path): CheckpointMetadata {
        DataInputStream(path.toFile().inputStream()).use { dis ->
            val magic = dis.readInt()
            require(magic == CONSTANTS.MAGIC_NUMBER) {
                "Invalid checkpoint file: bad magic number"
            }

            val version = dis.readInt()
            require(version == CONSTANTS.VERSION) {
                "Unsupported checkpoint version: $version"
            }

            return CheckpointMetadata(
                generation = dis.readInt(),
                fitness = dis.readFloat(),
                playstyle = Playstyle.valueOf(dis.readUTF()),
                timestamp = dis.readLong(),
                mutationSigma = dis.readFloat(),
                bestFitnessEver = dis.readFloat()
            )
        }
    }

    private object CONSTANTS {
        const val MAGIC_NUMBER = 0x41524E59
        const val VERSION = 1
    }
}
