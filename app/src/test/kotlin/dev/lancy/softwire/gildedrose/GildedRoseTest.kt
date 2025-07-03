package dev.lancy.softwire.gildedrose

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals

class GildedRoseTest {
    companion object {
        // / In practice, we shouldn't need to test any number greater than this.
        const val MAX_DAYS = 365

        // / By the specification, no item (except Sulfuras) can have a quality greater than 50.
        const val MAX_QUALITY = 50
    }

    @Property
    fun `quality degrades twice as fast after sell by date`(
        @ForAll("normalItem") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        // Clarification: "twice as fast" simply means a double in/decrement.
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality

        repeat(daysPassed) {
            app.updateQuality()
            val expectedDecrement = if (app.items[0].sellIn < 0) 2 else 1

            assertEquals(
                (lastQuality - expectedDecrement).coerceIn(0..Int.MAX_VALUE),
                app.items[0].quality,
                "Quality should degrade by 1 on day $it",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Provide
    fun anyItem(): Arbitrary<Item> =
        Arbitraries.oneOf(
            normalItem(),
            sulfuras(),
            backstagePasses(),
            agedBrie(),
        )

    @Provide
    fun normalItem(): Arbitrary<Item> {
        val names = names()
        val sellIns = sellIns()
        val qualities = qualities()

        return Combinators
            .combine(names, sellIns, qualities)
            .`as`(::Item)
    }

    @Provide
    fun sulfuras(): Arbitrary<Item> =
        Combinators
            .combine(listOf(sellIns()))
            .`as` { sellIn -> Item("Sulfuras, Hand of Ragnaros", sellIn[0], 80) }

    @Provide
    fun backstagePasses(): Arbitrary<Item> =
        Combinators
            .combine(names(), sellIns(), qualities())
            .`as` { name, sellIn, quality -> Item("Backstage passes to a $name concert", sellIn, quality) }

    @Provide
    fun agedBrie(): Arbitrary<Item> =
        Combinators
            .combine(sellIns(), qualities())
            .`as` { sellIn, quality -> Item("Aged Brie", sellIn, quality) }

    fun names(): Arbitrary<String> =
        Arbitraries
            .strings()
            .ofMinLength(1)
            // It should not be necessary to constrain the length, but it helps avoid
            // issues with the test framework's shrinking logic.
            .ofMaxLength(10)
            .filter {
                it != "Sulfuras, Hand of Ragnaros" &&
                    it != "Aged Brie" &&
                    !it.startsWith("Backstage passes")
            }

    fun sellIns(): Arbitrary<Int> =
        Arbitraries
            .integers()
            // Arbitrarily constrain to a year - intuitively shouldn't be necessary to go further!
            .between(0, MAX_DAYS)

    fun qualities(): Arbitrary<Int> =
        Arbitraries
            .integers()
            .between(0, MAX_QUALITY)
}
