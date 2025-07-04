package dev.lancy.softwire.gildedrose

/**
 * Each [GildedRule] updates the [Item.quality] and/or [Item.sellIn] properties of the [Item]s in a
 * specific way.
 */
sealed class GildedRule {
    /**
     * Updates the [Item.quality] and/or [Item.sellIn] properties of the [Item]s.
     */
    abstract fun update(item: Item)

    /**
     * Increments the [Item.quality] by the specified amount, ensuring it stays within bounds.
     *
     * @param multiplier The multiplier to apply to the increment.
     * @param postSellInMultiplier The multiplier to apply to the decrement after the [Item.sellIn] date has **passed**.
     */
    data class QualityDecrement(
        val multiplier: Int = 1,
        val postSellInMultiplier: Int = 2,
    ) : GildedRule() {
        override fun update(item: Item) {
            val delta = -GildedRose.INCREMENT *
                multiplier *
                if (item.sellIn <= 0) postSellInMultiplier else 1
            item modQual delta
        }
    }

    /**
     * Specifically for the "Backstage passes to a TAFKAL80ETC concert" item, the quality
     * - increases by 3 when there are 5 days or less to sell,
     * - increases by 2 when there are 10 days or less to sell,
     * - increases by 1 when there are more than 10 days to sell,
     * - drops to 0 when the sell-by date has passed.
     */
    data object BackstageQualityIncrement : GildedRule() {
        override fun update(item: Item) = when (item.sellIn) {
            in Int.MIN_VALUE..0 -> item.quality = 0
            in 1..5 -> item modQual GildedRose.INCREMENT * 3
            in 6..10 -> item modQual GildedRose.INCREMENT * 2
            else -> item modQual GildedRose.INCREMENT
        }
    }

    /**
     * Specifically for the "Aged Brie" item, the quality
     * - increases by 1 when the sell-by date has not passed,
     * - increases by 2 when the sell-by date has passed.
     */
    data object AgedBrieQualityIncrement : GildedRule() {
        override fun update(item: Item) {
            item modQual GildedRose.INCREMENT * if (item.sellIn <= 0) 2 else 1
        }
    }

    /**
     * Most items decrement their [Item.sellIn] value by 1 each day.
     *
     * The sole exception is the "Sulfuras, Hand of Ragnaros" item, which never has to be sold or
     * decreases in quality.
     */
    data object SellInDecrement : GildedRule() {
        override fun update(item: Item) {
            item.sellIn--
        }
    }
}
