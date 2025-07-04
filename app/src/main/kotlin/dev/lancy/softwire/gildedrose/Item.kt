package dev.lancy.softwire.gildedrose

import dev.lancy.softwire.gildedrose.GildedRose.Companion.MAX_QUALITY

data class Item(
    /**
     * Name of the item.
     */
    var name: String,
    /**
     * Number of days before the item has to be sold.
     */
    var sellIn: Int,
    /**
     * How valuable this item is.
     */
    var quality: Int,
)

/**
 * Changes the quality of the item by the specified amount, ensuring it stays positive and
 * within bounds.
 */
infix fun Item.modQual(by: Int) {
    this.quality = (this.quality + by).coerceIn(0..MAX_QUALITY)
}
