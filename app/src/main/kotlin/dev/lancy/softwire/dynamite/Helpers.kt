package dev.lancy.softwire.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move as JMove

enum class Move {
    ROCK,
    PAPER,
    SCISSORS,
    DYNAMITE,
    WATER,
    ;

    infix fun beats(other: Move): Boolean =
        when (this) {
            other -> false
            ROCK -> other == SCISSORS || other == DYNAMITE
            PAPER -> other == ROCK || other == DYNAMITE
            SCISSORS -> other == PAPER || other == DYNAMITE
            DYNAMITE -> other != WATER
            WATER -> other == DYNAMITE
        }

    val isBeatBy: Set<Move>
        get() =
            when (this) {
                ROCK -> setOf(PAPER, DYNAMITE)
                PAPER -> setOf(SCISSORS, DYNAMITE)
                SCISSORS -> setOf(ROCK, DYNAMITE)
                DYNAMITE -> setOf(WATER)
                WATER -> setOf(ROCK, PAPER, SCISSORS)
            }

    fun toJMove(): JMove = JMOVE_MAPPINGS.entries.first { it.value == this }.key

    companion object {
        val JMOVE_MAPPINGS =
            mapOf(
                JMove.R to ROCK,
                JMove.P to PAPER,
                JMove.S to SCISSORS,
                JMove.D to DYNAMITE,
                JMove.W to WATER,
            )
    }
}

abstract class LancyBot : Bot {
    override fun makeMove(state: Gamestate): JMove = play(state).toJMove()

    abstract fun play(state: Gamestate): Move

    inner class WinData {
        var wins: Int = 0
            internal set
        var draws: Int = 0
            internal set
        var outOf: Int = 0
            internal set

        fun probability(): Double = if (outOf == 0) 0.0 else wins.toDouble() / outOf
    }

    fun winData(state: Gamestate): Map<Move, WinData> =
        state
            .rounds
            .fold(Move.entries.associateWith { WinData() }) { map, round ->
                val data = map[round.p1.toMove()] ?: throw IllegalArgumentException("Unknown move: ${round.p1}")
                when {
                    round.p1 == round.p2 -> data.draws++
                    round.p1.toMove() beats round.p2.toMove() -> data.wins++
                }
                data.outOf++
                map
            }
}

fun JMove.toMove(): Move =
    Move.JMOVE_MAPPINGS[this]
        ?: throw IllegalArgumentException("Unknown JMove: $this")
