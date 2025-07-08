package dev.lancy.softwire.dynamite

import com.softwire.dynamite.game.Round
import java.util.EnumMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import com.softwire.dynamite.game.Move as JMove

fun JMove.toMove(): dev.lancy.softwire.dynamite.Move = Move.JMOVE_MAPPINGS[this]
    ?: throw IllegalArgumentException("Unknown JMove: $this")

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Boolean.fold(
    ifTrue: () -> T,
    ifFalse: () -> T,
): T {
    contract {
        callsInPlace(ifTrue, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
        callsInPlace(ifFalse, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }
    return if (this) { ifTrue() } else { ifFalse() }
}

fun List<Round>.toGameSnapshots(): List<GameSnapshot> {
    var p1DynamiteLeft = 100
    var p2DynamiteLeft = 100
    var p1RoundsSinceDynamite = 0
    var p2RoundsSinceDynamite = 0
    var p1RoundsSinceWater = 0
    var p2RoundsSinceWater = 0

    val p1MoveCounts = EnumMap<Move, Int>(Move::class.java)
    val p2MoveCounts = EnumMap<Move, Int>(Move::class.java)
    Move.entries.forEach { p1MoveCounts[it] = 0; p2MoveCounts[it] = 0 }

    return mapIndexed { idx, round ->
        val p1Move = round.p1.toMove()
        val p2Move = round.p2.toMove()

        // Update move counts
        p1MoveCounts[p1Move] = p1MoveCounts.getValue(p1Move) + 1
        p2MoveCounts[p2Move] = p2MoveCounts.getValue(p2Move) + 1

        // Calculate probabilities
        val p1TotalMoves = idx + 1f
        val p2TotalMoves = idx + 1f
        val p1Probs = EnumMap<Move, Float>(Move::class.java).apply {
            Move.entries.forEach { set(it, p1MoveCounts[it]!! / p1TotalMoves) }
        }
        val p2Probs = EnumMap<Move, Float>(Move::class.java).apply {
            Move.entries.forEach { set(it, p2MoveCounts[it]!! / p2TotalMoves) }
        }

        // Update dynamite counters
        if (p1Move == Move.DYNAMITE) {
            p1DynamiteLeft--
            p1RoundsSinceDynamite = 0
        } else {
            p1RoundsSinceDynamite++
        }
        if (p2Move == Move.DYNAMITE) {
            p2DynamiteLeft--
            p2RoundsSinceDynamite = 0
        } else {
            p2RoundsSinceDynamite++
        }
        if (p1Move == Move.WATER) p1RoundsSinceWater = 0 else p1RoundsSinceWater++
        if (p2Move == Move.WATER) p2RoundsSinceWater = 0 else p2RoundsSinceWater++

        GameSnapshot(
            rollover = idx,
            playerOne = GameSnapshot.PlayerSnapshot(
                move = p1Move,
                dynamiteLeft = p1DynamiteLeft,
                roundsSinceDynamite = p1RoundsSinceDynamite,
                roundsSinceWater = p1RoundsSinceWater,
                moveProbabilities = p1Probs,
            ),
            playerTwo = GameSnapshot.PlayerSnapshot(
                move = p2Move,
                dynamiteLeft = p2DynamiteLeft,
                roundsSinceDynamite = p2RoundsSinceDynamite,
                roundsSinceWater = p2RoundsSinceWater,
                moveProbabilities = p2Probs,
            ),
        )
    }
}
