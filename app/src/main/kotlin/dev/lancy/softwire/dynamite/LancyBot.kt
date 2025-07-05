package dev.lancy.softwire.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move as JMove

abstract class LancyBot : Bot {
    override fun makeMove(state: Gamestate): JMove = play(state).toJMove()

    abstract fun play(state: Gamestate): Move
}
