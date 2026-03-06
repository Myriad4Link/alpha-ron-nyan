package xyz.uthofficial.arnyan.simplified.network

import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDList
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import kotlin.math.sqrt

class SanmaPolicyNetwork {

    companion object {
        const val INPUT_CHANNELS = 11
        const val TILE_COUNT = 27
        const val ACTION_COUNT = 11
        const val HIDDEN_SIZE = 256
    }

    private var conv1Weight: NDArray? = null
    private var conv1Bias: NDArray? = null
    private var conv1BnMean: NDArray? = null
    private var conv1BnVar: NDArray? = null
    private var conv1BnGamma: NDArray? = null
    private var conv1BnBeta: NDArray? = null

    private var conv2Weight: NDArray? = null
    private var conv2Bias: NDArray? = null
    private var conv2BnMean: NDArray? = null
    private var conv2BnVar: NDArray? = null
    private var conv2BnGamma: NDArray? = null
    private var conv2BnBeta: NDArray? = null

    private var res1Conv1Weight: NDArray? = null
    private var res1Conv1BnMean: NDArray? = null
    private var res1Conv1BnVar: NDArray? = null
    private var res1Conv1BnGamma: NDArray? = null
    private var res1Conv1BnBeta: NDArray? = null
    private var res1Conv2Weight: NDArray? = null
    private var res1Conv2BnMean: NDArray? = null
    private var res1Conv2BnVar: NDArray? = null
    private var res1Conv2BnGamma: NDArray? = null
    private var res1Conv2BnBeta: NDArray? = null

    private var res2Conv1Weight: NDArray? = null
    private var res2Conv1BnMean: NDArray? = null
    private var res2Conv1BnVar: NDArray? = null
    private var res2Conv1BnGamma: NDArray? = null
    private var res2Conv1BnBeta: NDArray? = null
    private var res2Conv2Weight: NDArray? = null
    private var res2Conv2BnMean: NDArray? = null
    private var res2Conv2BnVar: NDArray? = null
    private var res2Conv2BnGamma: NDArray? = null
    private var res2Conv2BnBeta: NDArray? = null

    private var res3Conv1Weight: NDArray? = null
    private var res3Conv1BnMean: NDArray? = null
    private var res3Conv1BnVar: NDArray? = null
    private var res3Conv1BnGamma: NDArray? = null
    private var res3Conv1BnBeta: NDArray? = null
    private var res3Conv2Weight: NDArray? = null
    private var res3Conv2BnMean: NDArray? = null
    private var res3Conv2BnVar: NDArray? = null
    private var res3Conv2BnGamma: NDArray? = null
    private var res3Conv2BnBeta: NDArray? = null
    private var res3ShortcutWeight: NDArray? = null
    private var res3ShortcutBnMean: NDArray? = null
    private var res3ShortcutBnVar: NDArray? = null
    private var res3ShortcutBnGamma: NDArray? = null
    private var res3ShortcutBnBeta: NDArray? = null

    private var res4Conv1Weight: NDArray? = null
    private var res4Conv1BnMean: NDArray? = null
    private var res4Conv1BnVar: NDArray? = null
    private var res4Conv1BnGamma: NDArray? = null
    private var res4Conv1BnBeta: NDArray? = null
    private var res4Conv2Weight: NDArray? = null
    private var res4Conv2BnMean: NDArray? = null
    private var res4Conv2BnVar: NDArray? = null
    private var res4Conv2BnGamma: NDArray? = null
    private var res4Conv2BnBeta: NDArray? = null

    private var fc1Weight: NDArray? = null
    private var fc1Bias: NDArray? = null

    private var subjectHeadWeight: NDArray? = null
    private var subjectHeadBias: NDArray? = null
    private var actionHeadWeight: NDArray? = null
    private var actionHeadBias: NDArray? = null

    private val manager = NDManager.newBaseManager()
    private var isInitialized = false

    init {
        initializeWeights()
    }

    private fun initializeWeights() {
        if (isInitialized) return

        conv1Weight = xavierInit(32, 11, 3)
        conv1Bias = zeros(32)
        conv1BnMean = zeros(32)
        conv1BnVar = ones(32)
        conv1BnGamma = ones(32)
        conv1BnBeta = zeros(32)

        conv2Weight = xavierInit(64, 32, 3)
        conv2Bias = zeros(64)
        conv2BnMean = zeros(64)
        conv2BnVar = ones(64)
        conv2BnGamma = ones(64)
        conv2BnBeta = zeros(64)

        res1Conv1Weight = xavierInit(64, 64, 3)
        res1Conv1BnMean = zeros(64)
        res1Conv1BnVar = ones(64)
        res1Conv1BnGamma = ones(64)
        res1Conv1BnBeta = zeros(64)
        res1Conv2Weight = xavierInit(64, 64, 3)
        res1Conv2BnMean = zeros(64)
        res1Conv2BnVar = ones(64)
        res1Conv2BnGamma = ones(64)
        res1Conv2BnBeta = zeros(64)

        res2Conv1Weight = xavierInit(64, 64, 3)
        res2Conv1BnMean = zeros(64)
        res2Conv1BnVar = ones(64)
        res2Conv1BnGamma = ones(64)
        res2Conv1BnBeta = zeros(64)
        res2Conv2Weight = xavierInit(64, 64, 3)
        res2Conv2BnMean = zeros(64)
        res2Conv2BnVar = ones(64)
        res2Conv2BnGamma = ones(64)
        res2Conv2BnBeta = zeros(64)

        res3Conv1Weight = xavierInit(128, 64, 3)
        res3Conv1BnMean = zeros(128)
        res3Conv1BnVar = ones(128)
        res3Conv1BnGamma = ones(128)
        res3Conv1BnBeta = zeros(128)
        res3Conv2Weight = xavierInit(128, 128, 3)
        res3Conv2BnMean = zeros(128)
        res3Conv2BnVar = ones(128)
        res3Conv2BnGamma = ones(128)
        res3Conv2BnBeta = zeros(128)
        res3ShortcutWeight = xavierInit(128, 64, 1)
        res3ShortcutBnMean = zeros(128)
        res3ShortcutBnVar = ones(128)
        res3ShortcutBnGamma = ones(128)
        res3ShortcutBnBeta = zeros(128)

        res4Conv1Weight = xavierInit(128, 128, 3)
        res4Conv1BnMean = zeros(128)
        res4Conv1BnVar = ones(128)
        res4Conv1BnGamma = ones(128)
        res4Conv1BnBeta = zeros(128)
        res4Conv2Weight = xavierInit(128, 128, 3)
        res4Conv2BnMean = zeros(128)
        res4Conv2BnVar = ones(128)
        res4Conv2BnGamma = ones(128)
        res4Conv2BnBeta = zeros(128)

        fc1Weight = xavierInit(HIDDEN_SIZE, 128)
        fc1Bias = zeros(HIDDEN_SIZE)

        subjectHeadWeight = xavierInit(TILE_COUNT, HIDDEN_SIZE)
        subjectHeadBias = zeros(TILE_COUNT)
        actionHeadWeight = xavierInit(ACTION_COUNT, HIDDEN_SIZE)
        actionHeadBias = zeros(ACTION_COUNT)

        isInitialized = true
    }

    private fun xavierInit(outChannels: Int, inChannels: Int, kernelSize: Int = 1): NDArray {
        val fanIn = inChannels * kernelSize
        val fanOut = outChannels * kernelSize
        val std = sqrt(2.0 / (fanIn + fanOut))
        val values = FloatArray(outChannels * inChannels * kernelSize) {
            (kotlin.random.Random.nextGaussian() * std).toFloat()
        }
        return manager.create(values, Shape(outChannels.toLong(), inChannels.toLong(), kernelSize.toLong()))
    }

    private fun xavierInit(outSize: Int, inSize: Int): NDArray {
        val std = sqrt(2.0 / (inSize + outSize))
        val values = FloatArray(outSize * inSize) {
            (kotlin.random.Random.nextGaussian() * std).toFloat()
        }
        return manager.create(values, Shape(outSize.toLong(), inSize.toLong()))
    }

    private fun zeros(size: Int): NDArray = manager.zeros(Shape(size.toLong()))
    private fun ones(size: Int): NDArray = manager.ones(Shape(size.toLong()))

    fun forward(input: NDArray): Pair<NDArray, NDArray> {
        require(input.shape.shape[0] == 1L) { "Batch size must be 1" }
        require(input.shape.shape[1] == INPUT_CHANNELS.toLong()) { "Input channels must be $INPUT_CHANNELS" }
        require(input.shape.shape[2] == TILE_COUNT.toLong()) { "Input tiles must be $TILE_COUNT" }

        var x = input

        x = conv1d(x, conv1Weight!!, conv1Bias!!)
        x = batchNorm1d(x, conv1BnMean!!, conv1BnVar!!, conv1BnGamma!!, conv1BnBeta!!)
        x = relu(x)
        x = maxPool1d(x, 2, 2)

        x = conv1d(x, conv2Weight!!, conv2Bias!!)
        x = batchNorm1d(x, conv2BnMean!!, conv2BnVar!!, conv2BnGamma!!, conv2BnBeta!!)
        x = relu(x)
        x = maxPool1d(x, 2, 2)

        x = residualBlock(x, res1Conv1Weight!!, res1Conv1BnMean!!, res1Conv1BnVar!!, res1Conv1BnGamma!!, res1Conv1BnBeta!!,
            res1Conv2Weight!!, res1Conv2BnMean!!, res1Conv2BnVar!!, res1Conv2BnGamma!!, res1Conv2BnBeta!!, null)

        x = residualBlock(x, res2Conv1Weight!!, res2Conv1BnMean!!, res2Conv1BnVar!!, res2Conv1BnGamma!!, res2Conv1BnBeta!!,
            res2Conv2Weight!!, res2Conv2BnMean!!, res2Conv2BnVar!!, res2Conv2BnGamma!!, res2Conv2BnBeta!!, null)

        x = residualBlock(x, res3Conv1Weight!!, res3Conv1BnMean!!, res3Conv1BnVar!!, res3Conv1BnGamma!!, res3Conv1BnBeta!!,
            res3Conv2Weight!!, res3Conv2BnMean!!, res3Conv2BnVar!!, res3Conv2BnGamma!!, res3Conv2BnBeta!!,
            res3ShortcutWeight!!)

        x = residualBlock(x, res4Conv1Weight!!, res4Conv1BnMean!!, res4Conv1BnVar!!, res4Conv1BnGamma!!, res4Conv1BnBeta!!,
            res4Conv2Weight!!, res4Conv2BnMean!!, res4Conv2BnVar!!, res4Conv2BnGamma!!, res4Conv2BnBeta!!, null)

        x = x.mean(intArrayOf(2), true)
        x = x.flatten()

        x = linear(x, fc1Weight!!, fc1Bias!!)
        x = relu(x)

        val subjectLogits = linear(x, subjectHeadWeight!!, subjectHeadBias!!)
        val actionLogits = linear(x, actionHeadWeight!!, actionHeadBias!!)

        return subjectLogits.softmax(-1) to actionLogits.softmax(-1)
    }

    private fun conv1d(x: NDArray, weight: NDArray, bias: NDArray): NDArray {
        val kernelSize = weight.shape.shape[2].toInt()
        val padding = kernelSize / 2
        return x.convolve(weight, padding).add(bias.reshape(Shape(1, weight.shape.shape[0], 1)))
    }

    private fun batchNorm1d(x: NDArray, mean: NDArray, variance: NDArray, gamma: NDArray, beta: NDArray): NDArray {
        val std = variance.add(1e-5f).sqrt()
        val normalized = x.sub(mean.reshape(Shape(1, mean.shape.shape[0], 1))).div(std.reshape(Shape(1, std.shape.shape[0], 1)))
        return normalized.mul(gamma.reshape(Shape(1, gamma.shape.shape[0], 1))).add(beta.reshape(Shape(1, beta.shape.shape[0], 1)))
    }

    private fun maxPool1d(x: NDArray, kernelSize: Int, stride: Int): NDArray {
        return x.maxPool(arrayOf(1, kernelSize), arrayOf(1, stride))
    }

    private fun residualBlock(x: NDArray, conv1W: NDArray, bn1Mean: NDArray, bn1Var: NDArray, bn1Gamma: NDArray, bn1Beta: NDArray,
                              conv2W: NDArray, bn2Mean: NDArray, bn2Var: NDArray, bn2Gamma: NDArray, bn2Beta: NDArray,
                              shortcut: NDArray?): NDArray {
        var out = conv1d(x, conv1W, manager.zeros(Shape(conv1W.shape.shape[0].toInt().toLong())))
        out = batchNorm1d(out, bn1Mean, bn1Var, bn1Gamma, bn1Beta)
        out = relu(out)
        out = conv1d(out, conv2W, manager.zeros(Shape(conv2W.shape.shape[0].toInt().toLong())))
        out = batchNorm1d(out, bn2Mean, bn2Var, bn2Gamma, bn2Beta)

        val residual = shortcut?.let { conv1d(x, it, manager.zeros(Shape(it.shape.shape[0].toInt().toLong()))) } ?: x
        return relu(out.add(residual))
    }

    private fun linear(x: NDArray, weight: NDArray, bias: NDArray): NDArray {
        return x.matMul(weight.transpose()).add(bias)
    }

    private fun relu(x: NDArray): NDArray {
        return x.maximum(0f)
    }

    fun getWeights(): FloatArray {
        val weights = mutableListOf<Float>()
        arrayOf(
            conv1Weight, conv1Bias, conv1BnMean, conv1BnVar, conv1BnGamma, conv1BnBeta,
            conv2Weight, conv2Bias, conv2BnMean, conv2BnVar, conv2BnGamma, conv2BnBeta,
            res1Conv1Weight, res1Conv1BnMean, res1Conv1BnVar, res1Conv1BnGamma, res1Conv1BnBeta,
            res1Conv2Weight, res1Conv2BnMean, res1Conv2BnVar, res1Conv2BnGamma, res1Conv2BnBeta,
            res2Conv1Weight, res2Conv1BnMean, res2Conv1BnVar, res2Conv1BnGamma, res2Conv1BnBeta,
            res2Conv2Weight, res2Conv2BnMean, res2Conv2BnVar, res2Conv2BnGamma, res2Conv2BnBeta,
            res3Conv1Weight, res3Conv1BnMean, res3Conv1BnVar, res3Conv1BnGamma, res3Conv1BnBeta,
            res3Conv2Weight, res3Conv2BnMean, res3Conv2BnVar, res3Conv2BnGamma, res3Conv2BnBeta,
            res3ShortcutWeight, res3ShortcutBnMean, res3ShortcutBnVar, res3ShortcutBnGamma, res3ShortcutBnBeta,
            res4Conv1Weight, res4Conv1BnMean, res4Conv1BnVar, res4Conv1BnGamma, res4Conv1BnBeta,
            res4Conv2Weight, res4Conv2BnMean, res4Conv2BnVar, res4Conv2BnGamma, res4Conv2BnBeta,
            fc1Weight, fc1Bias,
            subjectHeadWeight, subjectHeadBias,
            actionHeadWeight, actionHeadBias
        ).forEach { arr ->
            arr?.toFloatArray()?.forEach { weights.add(it) }
        }
        return weights.toFloatArray()
    }

    fun setWeights(weights: FloatArray) {
        var idx = 0
        fun setArray(arr: NDArray): Int {
            val size = arr.size().toInt()
            arr.set(weights.sliceArray(idx until idx + size))
            idx += size
            return size
        }

        arrayOf(
            conv1Weight, conv1Bias, conv1BnMean, conv1BnVar, conv1BnGamma, conv1BnBeta,
            conv2Weight, conv2Bias, conv2BnMean, conv2BnVar, conv2BnGamma, conv2BnBeta,
            res1Conv1Weight, res1Conv1BnMean, res1Conv1BnVar, res1Conv1BnGamma, res1Conv1BnBeta,
            res1Conv2Weight, res1Conv2BnMean, res1Conv2BnVar, res1Conv2BnGamma, res1Conv2BnBeta,
            res2Conv1Weight, res2Conv1BnMean, res2Conv1BnVar, res2Conv1BnGamma, res2Conv1BnBeta,
            res2Conv2Weight, res2Conv2BnMean, res2Conv2BnVar, res2Conv2BnGamma, res2Conv2BnBeta,
            res3Conv1Weight, res3Conv1BnMean, res3Conv1BnVar, res3Conv1BnGamma, res3Conv1BnBeta,
            res3Conv2Weight, res3Conv2BnMean, res3Conv2BnVar, res3Conv2BnGamma, res3Conv2BnBeta,
            res3ShortcutWeight, res3ShortcutBnMean, res3ShortcutBnVar, res3ShortcutBnGamma, res3ShortcutBnBeta,
            res4Conv1Weight, res4Conv1BnMean, res4Conv1BnVar, res4Conv1BnGamma, res4Conv1BnBeta,
            res4Conv2Weight, res4Conv2BnMean, res4Conv2BnVar, res4Conv2BnGamma, res4Conv2BnBeta,
            fc1Weight, fc1Bias,
            subjectHeadWeight, subjectHeadBias,
            actionHeadWeight, actionHeadBias
        ).forEach { arr -> arr?.let { setArray(it) } }
    }

    fun countParameters(): Int {
        var count = 0
        arrayOf(
            conv1Weight, conv1Bias, conv1BnMean, conv1BnVar, conv1BnGamma, conv1BnBeta,
            conv2Weight, conv2Bias, conv2BnMean, conv2BnVar, conv2BnGamma, conv2BnBeta,
            res1Conv1Weight, res1Conv1BnMean, res1Conv1BnVar, res1Conv1BnGamma, res1Conv1BnBeta,
            res1Conv2Weight, res1Conv2BnMean, res1Conv2BnVar, res1Conv2BnGamma, res1Conv2BnBeta,
            res2Conv1Weight, res2Conv1BnMean, res2Conv1BnVar, res2Conv1BnGamma, res2Conv1BnBeta,
            res2Conv2Weight, res2Conv2BnMean, res2Conv2BnVar, res2Conv2BnGamma, res2Conv2BnBeta,
            res3Conv1Weight, res3Conv1BnMean, res3Conv1BnVar, res3Conv1BnGamma, res3Conv1BnBeta,
            res3Conv2Weight, res3Conv2BnMean, res3Conv2BnVar, res3Conv2BnGamma, res3Conv2BnBeta,
            res3ShortcutWeight, res3ShortcutBnMean, res3ShortcutBnVar, res3ShortcutBnGamma, res3ShortcutBnBeta,
            res4Conv1Weight, res4Conv1BnMean, res4Conv1BnVar, res4Conv1BnGamma, res4Conv1BnBeta,
            res4Conv2Weight, res4Conv2BnMean, res4Conv2BnVar, res4Conv2BnGamma, res4Conv2BnBeta,
            fc1Weight, fc1Bias,
            subjectHeadWeight, subjectHeadBias,
            actionHeadWeight, actionHeadBias
        ).forEach { arr -> count += arr?.size()?.toInt() ?: 0 }
        return count
    }

    fun saveBinary(path: Path) {
        path.parent?.toFile()?.mkdirs()
        DataOutputStream(path.toFile().outputStream()).use { dos ->
            dos.writeInt(countParameters())
            getWeights().forEach { dos.writeFloat(it) }
        }
    }

    fun loadBinary(path: Path) {
        DataInputStream(path.toFile().inputStream()).use { dis ->
            val count = dis.readInt()
            val weights = FloatArray(count) { dis.readFloat() }
            setWeights(weights)
        }
    }

    fun close() {
        manager.close()
    }

    protected fun finalize() {
        close()
    }
}

private fun NDArray.convolve(weight: NDArray, padding: Int): NDArray {
    val inputShape = this.shape.shape
    val weightShape = weight.shape.shape
    val batchSize = inputShape[0].toInt()
    val outChannels = weightShape[0].toInt()
    val inChannels = weightShape[1].toInt()
    val kernelSize = weightShape[2].toInt()
    val seqLen = inputShape[2].toInt()
    val outLen = seqLen + 2 * padding - kernelSize + 1

    val inputData = this.toFloatArray()
    val weightData = weight.toFloatArray()
    val outputData = FloatArray(batchSize * outChannels * outLen)

    for (b in 0 until batchSize) {
        for (oc in 0 until outChannels) {
            for (i in 0 until outLen) {
                var sum = 0f
                for (ic in 0 until inChannels) {
                    for (k in 0 until kernelSize) {
                        val inputIdx = i + k - padding
                        if (inputIdx in 0 until seqLen) {
                            val wIdx = oc * inChannels * kernelSize + ic * kernelSize + k
                            val xIdx = b * inChannels * seqLen + ic * seqLen + inputIdx
                            sum += weightData[wIdx] * inputData[xIdx]
                        }
                    }
                }
                val outIdx = b * outChannels * outLen + oc * outLen + i
                outputData[outIdx] = sum
            }
        }
    }

    return this.manager.create(outputData, Shape(batchSize.toLong(), outChannels.toLong(), outLen.toLong()))
}

private fun NDArray.maxPool(indices: Array<Int>, strides: Array<Int>): NDArray {
    val shape = this.shape.shape
    val batchSize = shape[0].toInt()
    val channels = shape[1].toInt()
    val seqLen = shape[2].toInt()
    val kernelSize = indices[1]
    val stride = strides[1]
    val outLen = (seqLen - kernelSize) / stride + 1

    val inputData = this.toFloatArray()
    val outputData = FloatArray(batchSize * channels * outLen)

    for (b in 0 until batchSize) {
        for (c in 0 until channels) {
            for (i in 0 until outLen) {
                var maxVal = Float.NEGATIVE_INFINITY
                for (k in 0 until kernelSize) {
                    val idx = i * stride + k
                    if (idx < seqLen) {
                        val inIdx = b * channels * seqLen + c * seqLen + idx
                        val v = inputData[inIdx]
                        if (v > maxVal) maxVal = v
                    }
                }
                val outIdx = b * channels * outLen + c * outLen + i
                outputData[outIdx] = maxVal
            }
        }
    }

    return this.manager.create(outputData, Shape(batchSize.toLong(), channels.toLong(), outLen.toLong()))
}

private fun kotlin.random.Random.nextGaussian(): Double {
    val u1 = this.nextFloat().toDouble()
    val u2 = this.nextFloat().toDouble()
    return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1.coerceAtLeast(1e-10))) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
}
