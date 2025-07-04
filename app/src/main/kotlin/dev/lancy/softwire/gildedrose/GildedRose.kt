package dev.lancy.softwire.gildedrose

class GildedRose(
    val items: List<Item>,
) {
    companion object {
        /// Increment by which to change quality normally.
        const val INCREMENT = 1

        /// Maximum quality an item can ever have, except for "Sulfuras, Hand of Ragnaros".
        const val MAX_QUALITY = 50
    }

    /**
     * Updates the quality and sellIn values of all items in the [items] list.
     *
     * @warning This method is mutating on individual elements of the [items] list.
     */
    fun updateQuality() = items.forEach(::updateItem)

    /**
     * Updates the [Item.quality] and [Item.sellIn] values of a single [Item].
     *
     * @param item The item to update.
     */
    private fun updateItem(item: Item) {
        // This check is here because we are not allowed to modify [Item].
        require(item.quality >= 0) { "Quality can never be negative!" }

        // Handle quality changes, **before** decrementing [sellIn].
        when (item.name) {
            // Aged Brie increases in quality the older it gets.
            // Disambiguate: Increment doubles **after** [sellIn] date.
            "Aged Brie" -> item modQual INCREMENT * if (item.sellIn <= 0) 2 else 1
            // Sulfuras does not change, ever.
            "Sulfuras, Hand of Ragnaros" -> return
            // Backstage passes increase in quality as the concert date approaches.
            "Backstage passes to a TAFKAL80ETC concert" -> when {
                // Quality is zeroed *after* the concert.
                item.sellIn <= 0 -> item.quality = 0
                item.sellIn < 6 -> item modQual INCREMENT * 3
                item.sellIn < 11 -> item modQual INCREMENT * 2
                else -> item modQual INCREMENT
            }
            // Kotlin has no real pattern matching... :)
            item.name if item.name.startsWith("Conjured") ->
                // Conjured items decrease in quality twice as fast.
                item modQual -INCREMENT * if (item.sellIn <= 0) 4 else 2
            else ->
                // All other items decrease in quality.
                item modQual -INCREMENT * if (item.sellIn <= 0) 2 else 1
        }

        // Finally, decrease the [sellIn] value for the item.
        if (item.name != "Sulfuras, Hand of Ragnaros") item.sellIn--
    }

    /// Changes the quality of the item by the specified amount, ensuring it stays positive and within bounds.
    private infix fun Item.modQual(by: Int) {
        this.quality = (this.quality + by).coerceIn(0..MAX_QUALITY)
    }
}
