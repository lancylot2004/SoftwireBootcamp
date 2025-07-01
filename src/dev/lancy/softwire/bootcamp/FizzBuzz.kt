package dev.lancy.softwire.bootcamp

fun main() {
    (1..100).forEach {
        buildString {
            if (it % 3 == 0) append("Fizz")
            if (it % 5 == 0) append("Buzz")

            if (isEmpty()) append(it)
        }.let(::println)
    }
}
