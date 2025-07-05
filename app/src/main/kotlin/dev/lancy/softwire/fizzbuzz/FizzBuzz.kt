package dev.lancy.softwire.fizzbuzz

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
        // is output regardless of above rule for 11.
        if (it % 13 == 0) {
            var index = items.indexOfFirst { item -> item.startsWith('B') }
            if (index == -1) index = items.size

            items.add(index, "Fezz")
        }

        if (it % 17 == 0) items.reverse()

        items
            .joinToString(separator = "")
            .ifEmpty { it }
            .let(::println)
    }
}
