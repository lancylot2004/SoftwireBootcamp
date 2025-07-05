package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Move as JMove

enum class Move {
    ROCK,
    PAPER,
    SCISSORS,
    DYNAMITE,
    WATER,
    ;

    infix fun beats(other: Move): Boolean = when (this) {
        other -> false
        ROCK -> other == SCISSORS || other == DYNAMITE
        PAPER -> other == ROCK || other == DYNAMITE
        SCISSORS -> other == PAPER || other == DYNAMITE
        DYNAMITE -> other != WATER
        WATER -> other == DYNAMITE
    }

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
    }
}

fun JMove.toMove(): dev.lancy.softwire.dynamite.Move = Move.JMOVE_MAPPINGS[this]
    ?: throw IllegalArgumentException("Unknown JMove: $this")
