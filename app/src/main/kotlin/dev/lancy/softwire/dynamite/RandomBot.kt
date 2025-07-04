package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

class RandomBot : LancyBot() {
    override fun play(state: Gamestate): Move = Move.entries.shuffled().first()
}
