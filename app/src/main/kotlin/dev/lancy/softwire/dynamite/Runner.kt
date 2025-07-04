package dev.lancy.softwire.dynamite

import com.softwire.dynamite.runner.DynamiteRunner

fun main() {
    DynamiteRunner.playGames { WeightedWinProbBot() }
}
