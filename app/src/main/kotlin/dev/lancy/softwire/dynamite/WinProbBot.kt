package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Gamestate

class WinProbBot : LancyBot() {
    companion object {
        const val RESEARCH_UNTIL = 300
    }

    private var round = 0

    override fun play(state: Gamestate): Move {
        round++
        return if (round < RESEARCH_UNTIL) {
            Move.entries.shuffled().first()
        } else {
            winData(state)
                .maxBy { it.value.probability() }
                .key.isBeatBy
                .shuffled()
                .first()
        }
    }
}
