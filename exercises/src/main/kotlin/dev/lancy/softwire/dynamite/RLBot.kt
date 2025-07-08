package dev.lancy.softwire.dynamite

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import java.nio.FloatBuffer
import java.nio.file.Files
import com.softwire.dynamite.game.Move as JMove

class RLBot(modelPath: String, private val windowSize: Int) : Bot {
    private val env = OrtEnvironment.getEnvironment()
    private val session = loadOnnxModelFromResources(modelPath, env)

    override fun makeMove(state: Gamestate): JMove {
        val snapshots = state.rounds.toGameSnapshots()

        val (history, currentState) = prepareModelInputs(snapshots, windowSize, env)
        val inputs = mapOf(
            "history_input" to history,
            "state_input" to currentState,
        )

        session.run(inputs).use { result ->
            val logits = (result[0].value as Array<FloatArray>)[0]
            val bestIndex = logits.indices.maxByOrNull { logits[it] } ?: 0
            return Move.fromIndex(bestIndex).toJMove()
        }
    }

    private fun loadOnnxModelFromResources(resourcePath: String, env: OrtEnvironment): OrtSession {
        val modelStream = object {}.javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Model not found in resources: $resourcePath")

        val tempFile = Files.createTempFile("dynamite_model", ".onnx").toFile()
        tempFile.outputStream().use { modelStream.copyTo(it) }

        return env.createSession(tempFile.absolutePath, OrtSession.SessionOptions())
    }

    private fun prepareModelInputs(
        snapshots: List<GameSnapshot>,
        windowSize: Int,
        env: OrtEnvironment,
    ): Pair<OnnxTensor, OnnxTensor> {
        val paddedSnapshots = snapshots.padHistory(windowSize)
        val history = paddedSnapshots.flatMap { it.combinedFeatures().asList() }.toFloatArray()

        val lastState = paddedSnapshots.last().stateFeatures()

        val historyTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(history),
            longArrayOf(1, windowSize.toLong(), 28),
        )

        val stateTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(lastState),
            longArrayOf(1, 3),
        )

        return historyTensor to stateTensor
    }

    private fun List<GameSnapshot>.padHistory(windowSize: Int): List<GameSnapshot> {
        val padCount = (windowSize - size).coerceAtLeast(0)
        val neutral = GameSnapshot(
            rollover = 0,
            playerOne = GameSnapshot.PlayerSnapshot.neutral(),
            playerTwo = GameSnapshot.PlayerSnapshot.neutral(),
        )
        return List(padCount) { neutral } + takeLast(windowSize)
    }
}
