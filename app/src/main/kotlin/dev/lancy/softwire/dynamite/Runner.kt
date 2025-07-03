package dev.lancy.softwire.dynamite

import com.softwire.dynamite.runner.DynamiteRunner

object Runner {
    @JvmStatic
    fun main() {
        DynamiteRunner.playGames { TestBot() }
    }
}
