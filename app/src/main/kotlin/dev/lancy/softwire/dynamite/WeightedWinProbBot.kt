package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Gamestate

class WeightedWinProbBot : LancyBot() {
    companion object {
        const val RESEARCH_UNTIL = 300

        val WEIGHTED_MOVES by lazy {
            Move.entries
                .zip(listOf(30, 30, 30, 5, 5))
                .flatMap { (move, count) -> List(count) { move } }
        }
    }

    private var round = 0

    override fun play(state: Gamestate): Move {
        round++
        return if (round < RESEARCH_UNTIL) {
            WEIGHTED_MOVES.shuffled().first()
        } else {
            winData(state)
                .maxBy { it.value.probability() }
                .key.isBeatBy
                .shuffled()
                .first()
        }
    }
}
