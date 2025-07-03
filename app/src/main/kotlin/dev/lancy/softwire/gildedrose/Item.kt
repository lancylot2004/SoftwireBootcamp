package dev.lancy.softwire.gildedrose

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
