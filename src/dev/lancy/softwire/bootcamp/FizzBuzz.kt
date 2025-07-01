package dev.lancy.softwire.bootcamp

const val DEFAULT_UP_TO = 100

fun main() {
    print("Execute FizzBuzz up to [$DEFAULT_UP_TO]: ")
    val upTo = readlnOrNull()?.toIntOrNull() ?: DEFAULT_UP_TO

    (1..upTo).forEach {
        val items = mutableListOf<String>()

        if (it % 11 == 0) {
            // 11 displaces all (most) other outputs.
            items += "Bong"
        } else {
            if (it % 3 == 0) items += "Fizz"
            if (it % 5 == 0) items += "Buzz"
            if (it % 7 == 0) items += "Bang"
        }

        // "Fezz" is put just before the first item that starts with 'B', and
        // is output regardless of the rule where 11 displaces other outputs.
        if (it % 13 == 0) {
            val addAt = when (val index = items.indexOfFirst { item -> item.startsWith('B') }) {
                // If not found, insert at end.
                -1 -> items.size
                else -> index
            }

            items.add(addAt, "Fezz")
        }

        if (it % 17 == 0) items.reverse()

        items
            .joinToString(separator = "")
            .ifEmpty { it }
            .let(::println)
    }
}
