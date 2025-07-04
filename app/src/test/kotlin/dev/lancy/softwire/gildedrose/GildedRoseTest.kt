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
        /// In practice, we shouldn't need to test any number greater than this.
        const val MAX_DAYS = 365
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
                (lastQuality - expectedDecrement).coerceQuality(),
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
            // Stop testing if we reach the sell by date.
            if (app.items[0].sellIn == 0) return@repeat

            app.updateQuality()

            assertEquals(
                (lastQuality + 1).coerceQuality(),
                app.items[0].quality,
                "Aged brie should have its [quality] increased by 1 every day before the sell by date.",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Property
    fun `aged brie doubly increases in quality after sellIn date`(
        @ForAll("agedBrie") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality

        // Force the item to be past its sell by date.
        item.sellIn = 0

        repeat(daysPassed) {
            app.updateQuality()

            assertEquals(
                (lastQuality + 2).coerceQuality(),
                app.items[0].quality,
                "Aged brie should have its [quality] increased by 2 every day after the sell by date.",
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
    fun `backstage passes increase in quality in a special way`(
        @ForAll("backstagePasses") item: Item,
        @ForAll @IntRange(min = 0, max = MAX_DAYS) daysPassed: Int,
    ) {
        val app = GildedRose(listOf(item))
        var lastQuality = item.quality
        var lastSellIn = item.sellIn

        repeat(daysPassed) {
            app.updateQuality()
            val (expectedIncrement, errorMessage) = when (lastSellIn) {
                // Quality should be zeroed after concert.
                in Int.MIN_VALUE..0 -> -lastQuality to "Backstage pass should have zero [quality] after the concert."
                in 1..5 ->
                    3 to "Backstage pass should have its [quality] increased by 3 when there are 5 " +
                        "or fewer days left to the concert."
                in 6..10 ->
                    2 to "Backstage pass should have its [quality] increased by 2 when there are 10 or fewer days " +
                        "left to the concert."
                else ->
                    1 to "Backstage pass should have its [quality] increased by 1 when there are more than 10 days " +
                        "left to the concert."
            }

            assertEquals(
                (lastQuality + expectedIncrement).coerceQuality(),
                app.items[0].quality,
                errorMessage,
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
                (lastSellIn - 1),
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
                (lastQuality - expectedDecrement).coerceQuality(),
                app.items[0].quality,
                "Conjured items should have their [quality] decreased by 2 every day, or 4 after sell by date.",
            )

            lastQuality = app.items[0].quality
        }
    }

    @Provide
    @Suppress("unused")
    private fun anyItemExceptSulfuras(): Arbitrary<Item> =
        Arbitraries.oneOf(
            normalItem(),
            backstagePasses(),
            agedBrie(),
        )

    @Provide
    private fun normalItem(): Arbitrary<Item> {
        val names = names()
        val sellIns = sellIns()
        val qualities = qualities()

        return Combinators
            .combine(names, sellIns, qualities)
            .`as`(::Item)
    }

    @Provide
    @Suppress("unused")
    private fun sulfuras(): Arbitrary<Item> =
        Combinators
            .combine(listOf(sellIns()))
            .`as` { sellIn -> Item("Sulfuras, Hand of Ragnaros", sellIn[0], 80) }

    @Provide
    private fun backstagePasses(): Arbitrary<Item> =
        Combinators
            .combine(sellIns(), qualities())
            .`as` { sellIn, quality -> Item("Backstage passes to a TAFKAL80ETC concert", sellIn, quality) }

    @Provide
    private fun agedBrie(): Arbitrary<Item> =
        Combinators
            .combine(sellIns(), qualities())
            .`as` { sellIn, quality -> Item("Aged Brie", sellIn, quality) }

    @Provide
    @Suppress("unused")
    private fun conjuredItems(): Arbitrary<Item> =
        normalItem().map {
            it.name = "Conjured ${it.name}"
            it
        }

    private fun names(): Arbitrary<String> =
        Arbitraries
            .strings()
            .ofMinLength(1)
            // Technically not necessary to constrain the length, but it
            // helps avoid issues with shrinking logic.
            .ofMaxLength(10)
            .filter {
                it != "Sulfuras, Hand of Ragnaros" &&
                    it != "Aged Brie" &&
                    !it.startsWith("Backstage passes")
            }

    private fun sellIns(): Arbitrary<Int> =
        Arbitraries
            .integers()
            .between(0, MAX_DAYS)

    private fun qualities(): Arbitrary<Int> =
        Arbitraries
            .integers()
            .between(0, GildedRose.MAX_QUALITY)

    private fun Int.coerceQuality(): Int = this.coerceIn(0..GildedRose.MAX_QUALITY)
}
