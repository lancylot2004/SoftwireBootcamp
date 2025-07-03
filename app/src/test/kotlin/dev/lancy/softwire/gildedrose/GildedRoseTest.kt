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

        // / By the specification, no item (except Sulfuras) can have a greater quality.
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
                "All items should have their [quality] decreased by 1 every day, with some exceptions.",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Property
    fun `aged brie increases in quality`(
        @ForAll("agedBrie") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality

        repeat(daysPassed) {
            app.updateQuality()
            val expectedIncrement = if (app.items[0].sellIn < 0) 2 else 1

            assertEquals(
                (lastQuality + expectedIncrement).coerceIn(0..MAX_QUALITY),
                app.items[0].quality,
                "Aged brie should have its [quality] increased by 1 (or 2 after sell by date) every day.",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Property
    fun `sulfuras does not change`(
        @ForAll("sulfuras") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        val initialQuality = item.quality
        val initialSellIn = item.sellIn
        assertEquals(
            80,
            app.items[0].quality,
            "[quality] for Sulfuras should be 80 and never change.",
        )

        repeat(daysPassed) {
            app.updateQuality()

            assertEquals(
                initialQuality,
                app.items[0].quality,
                "[quality] for Sulfuras should be 80 and never change.",
            )
            assertEquals(
                initialSellIn,
                app.items[0].sellIn,
                "[sellIn] for Sulfuras should never change.",
            )
        }
    }

    @Property
    fun `backstage passes increase in quality more as concert approaches but is zeroed after concert`(
        @ForAll("backstagePasses") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality
        var lastSellIn = item.sellIn

        repeat(daysPassed) {
            app.updateQuality()
            val expectedIncrement =
                when (lastSellIn) {
                    // Quality should be zeroed after concert.
                    in Int.MIN_VALUE..0 -> -lastQuality
                    in 1..5 -> 3
                    in 6..10 -> 2
                    else -> 1
                }

            assertEquals(
                (lastQuality + expectedIncrement).coerceIn(0..MAX_QUALITY),
                app.items[0].quality,
                "Backstage pass should have its [quality] change by $expectedIncrement from $lastQuality on " +
                    "day $it, when there are ${item.sellIn} days left to the concert.",
            )

            lastQuality = app.items[0].quality
            lastSellIn = app.items[0].sellIn
        }
    }

    @Property
    fun `sellIn decreases by 1 every day except for Sulfuras`(
        @ForAll("anyItemExceptSulfuras") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastSellIn = item.sellIn

        repeat(daysPassed) {
            app.updateQuality()

            assertEquals(
                (lastSellIn - 1).coerceIn(Int.MIN_VALUE..Int.MAX_VALUE),
                app.items[0].sellIn,
                "All items should have their [sellIn] decreased by 1 every day, except for Sulfuras.",
            )

            lastSellIn = app.items[0].sellIn
        }
    }

    @Property
    fun `conjured items degrade in quality twice as fast`(
        @ForAll("conjuredItems") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality

        repeat(daysPassed) {
            app.updateQuality()
            val expectedDecrement = if (app.items[0].sellIn < 0) 4 else 2

            assertEquals(
                (lastQuality - expectedDecrement).coerceIn(0..Int.MAX_VALUE),
                app.items[0].quality,
                "Conjured items should have their [quality] decreased by 2 every day, or 4 after sell by date.",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Provide
    fun anyItemExceptSulfuras(): Arbitrary<Item> =
        Arbitraries.oneOf(
            normalItem(),
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
            .combine(sellIns(), qualities())
            .`as` { sellIn, quality -> Item("Backstage passes to a TAFKAL80ETC concert", sellIn, quality) }

    @Provide
    fun agedBrie(): Arbitrary<Item> =
        Combinators
            .combine(sellIns(), qualities())
            .`as` { sellIn, quality -> Item("Aged Brie", sellIn, quality) }

    @Provide
    fun conjuredItems(): Arbitrary<Item> =
        normalItem().map {
            it.name = "Conjured ${it.name}"
            it
        }

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
