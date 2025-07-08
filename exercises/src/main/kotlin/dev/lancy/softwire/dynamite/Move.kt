package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Move as JMove

enum class Move {
    ROCK,
    PAPER,
    SCISSORS,
    DYNAMITE,
    WATER,
    ;

    fun toJMove(): JMove = JMOVE_MAPPINGS.entries.first { it.value == this }.key

    fun toOneHot(): FloatArray = FloatArray(entries.size) { if (it == ordinal) 1f else 0f }

    companion object {
        val JMOVE_MAPPINGS = mapOf(
            JMove.R to ROCK,
            JMove.P to PAPER,
            JMove.S to SCISSORS,
            JMove.D to DYNAMITE,
            JMove.W to WATER,
        )

        fun fromIndex(index: Int): Move = entries[index]
    }
}
