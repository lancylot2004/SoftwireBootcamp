package dev.lancy.softwire.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import io.kinference.core.KIEngine
import io.kinference.core.data.tensor.asTensor
import io.kinference.core.model.KIModel
import io.kinference.ndarray.arrays.FloatNDArray
import kotlinx.coroutines.runBlocking
import com.softwire.dynamite.game.Move as JMove

private const val HISTORY_INPUT = "history_input"
private const val STATE_INPUT = "state_input"
private const val OUTPUT_NAME = "output"

class RLBot(
    modelPath: String,
    private val windowSize: Int,
) : Bot {
    private val model = runBlocking { loadOnnxModelFromResources(modelPath) }

    override fun makeMove(state: Gamestate): JMove {
        val snapshots = state.rounds.toGameSnapshots()
        val (historyInput, stateInput) = runBlocking { prepareModelInputs(snapshots, windowSize) }

        val inputTensors =
            listOf(
                historyInput.asTensor(HISTORY_INPUT),
                stateInput.asTensor(STATE_INPUT),
            )

        val output = runBlocking { model.predict(inputTensors) }

        val logitsTensor = output[OUTPUT_NAME]?.data
        requireNotNull(logitsTensor) { "[Logit Output Missing] Check `OUTPUT_NAME`?" }
        require(logitsTensor is FloatNDArray) { "[Unexpected Logit Output] ${logitsTensor::class}" }
        require(logitsTensor.shape.contentEquals(intArrayOf(1, 5))) { "[Logit Dimension Mismatch] Expected vector of 5." }

        val bestMoveIdx = runBlocking { logitsTensor.argmax(1).getLinear(0) }
        return Move.fromIndex(bestMoveIdx).toJMove()
    }

    private suspend fun loadOnnxModelFromResources(resourcePath: String): KIModel {
        val resourceStream =
            object {}.javaClass.getResourceAsStream(resourcePath)
                ?: error("Resource not found: $resourcePath")

        val tempFile =
            kotlin.io.path
                .createTempFile(suffix = ".onnx")
                .toFile()
        tempFile.deleteOnExit()
        resourceStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return KIEngine.loadModel(tempFile.absolutePath, optimize = true)
    }

    private fun prepareModelInputs(
        snapshots: List<GameSnapshot>,
        windowSize: Int,
    ): Pair<FloatNDArray, FloatNDArray> {
        val featureSize = snapshots.firstOrNull()?.combinedFeatures()?.size ?: 28
        val history: FloatArray
        val lastState: FloatArray

        if (snapshots.isEmpty()) {
            val neutral =
                GameSnapshot.PlayerSnapshot.neutral().aggregate(0f) +
                    GameSnapshot.PlayerSnapshot.neutral().aggregate(0f)
            history = FloatArray(windowSize * neutral.size) { 0f }
            lastState = neutral
        } else {
            val paddedSnapshots = snapshots.padHistory(windowSize)
            history = paddedSnapshots.flatMap { it.combinedFeatures().asList() }.toFloatArray()
            lastState = snapshots.last().combinedFeatures()
        }

        if (history.isEmpty()) throw IllegalStateException("History features array is empty")
        if (lastState.isEmpty()) throw IllegalStateException("State features array is empty")

        val historyNDArray = FloatNDArray(1, windowSize, featureSize) { idx: Int -> history[idx] }
        val stateNDArray = FloatNDArray(1, featureSize) { idx: Int -> lastState[idx] }

        return historyNDArray to stateNDArray
    }

    private fun List<GameSnapshot>.padHistory(windowSize: Int): List<GameSnapshot> {
        val padCount = (windowSize - size).coerceAtLeast(0)
        val neutral =
            GameSnapshot(
                rollover = 0,
                playerOne = GameSnapshot.PlayerSnapshot.neutral(),
                playerTwo = GameSnapshot.PlayerSnapshot.neutral(),
            )
        return List(padCount) { neutral } + takeLast(windowSize)
    }
}
