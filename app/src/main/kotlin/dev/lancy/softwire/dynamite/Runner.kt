package dev.lancy.softwire.dynamite

import com.softwire.dynamite.runner.DynamiteRunner

fun main(args: Array<String>) {
    DynamiteRunner.playGames { RandomBot() }
}
