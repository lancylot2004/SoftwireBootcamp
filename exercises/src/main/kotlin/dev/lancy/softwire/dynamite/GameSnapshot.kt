package dev.lancy.softwire.dynamite

import java.util.*

data class GameSnapshot(
    val rollover: Int,
    val playerOne: PlayerSnapshot,
    val playerTwo: PlayerSnapshot,
) {
    companion object {
        const val MAX_ROLLOVER = 1000f
        const val MAX_GAME_LENGTH = 2500f
        const val MAX_DYNAMITE = 100f
    }

    data class PlayerSnapshot(
        val move: Move,
        val dynamiteLeft: Int,
        val roundsSinceDynamite: Int,
        val roundsSinceWater: Int,
        val moveProbabilities: EnumMap<Move, Float>,
    ) {
        fun aggregate(rollover: Float): FloatArray {
            val rolloverNorm = (rollover / MAX_ROLLOVER).coerceAtMost(1.0f)
            val dynamiteLeftNorm = dynamiteLeft / MAX_DYNAMITE
            val roundsSinceDynamiteNorm = (roundsSinceDynamite / MAX_GAME_LENGTH).coerceAtMost(1.0f)
            val roundsSinceWaterNorm = (roundsSinceWater / MAX_GAME_LENGTH).coerceAtMost(1.0f)
            val orderedProbabilities = Move.entries.map { moveProbabilities[it] ?: 0.0f }.toFloatArray()

            return floatArrayOf(
                rolloverNorm,
                dynamiteLeftNorm,
                roundsSinceDynamiteNorm,
                roundsSinceWaterNorm,
            ) + move.toOneHot() + orderedProbabilities
        }

        companion object {
            fun neutral(): PlayerSnapshot = PlayerSnapshot(
                move = Move.ROCK,
                dynamiteLeft = MAX_DYNAMITE.toInt(),
                roundsSinceDynamite = 0,
                roundsSinceWater = 0,
                moveProbabilities = EnumMap<Move, Float>(Move::class.java).apply {
                    Move.entries.forEach { set(it, 0.2f) }
                },
            )
        }
    }

    fun stateFeatures(): FloatArray = floatArrayOf(
        playerOne.dynamiteLeft / MAX_DYNAMITE,
        playerTwo.dynamiteLeft / MAX_DYNAMITE,
        (rollover / MAX_ROLLOVER).coerceAtMost(1.0f),
    )

    fun combinedFeatures(): FloatArray =
        playerOne.aggregate(rollover.toFloat()) + playerTwo.aggregate(rollover.toFloat())
}
