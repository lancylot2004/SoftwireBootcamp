package dev.lancy.softwire.dynamite

import java.util.*

data class GameSnapshot(
    val rollover: Int,
    val playerOne: PlayerSnapshot,
    val playerTwo: PlayerSnapshot,
) {
    data class PlayerSnapshot(
        val move: Move,
        val dynamiteLeft: Int,
        val roundsSinceDynamite: Int,
        val roundsSinceWater: Int,
        val moveProbabilities: EnumMap<Move, Float>,
    ) {
        fun aggregate(rollover: Float): FloatArray =
            floatArrayOf(
                rollover,
                dynamiteLeft.toFloat(),
                roundsSinceDynamite.toFloat(),
                roundsSinceWater.toFloat(),
            ) + move.toOneHot() + moveProbabilities.values.toFloatArray()

        companion object {
            fun neutral(): PlayerSnapshot = PlayerSnapshot(
                move = Move.ROCK,
                dynamiteLeft = 100,
                roundsSinceDynamite = Int.MAX_VALUE,
                roundsSinceWater = Int.MAX_VALUE,
                moveProbabilities = EnumMap<Move, Float>(Move::class.java).apply {
                    Move.entries.forEach { set(it, 1f / Move.entries.size) }
                },
            )
        }
    }

    fun combinedFeatures(): FloatArray =
        playerOne.aggregate(rollover.toFloat()) + playerTwo.aggregate(rollover.toFloat())
}
