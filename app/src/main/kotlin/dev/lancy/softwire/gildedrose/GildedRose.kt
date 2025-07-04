package dev.lancy.softwire.gildedrose

class GildedRose(
    val items: List<Item>,
) {
    companion object {
        /**
         * Increment by which to change quality normally.
         */
        const val INCREMENT = 1

        /**
         * Maximum quality an item can ever have, except for "Sulfuras, Hand of Ragnaros".
         */
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

        // Ordering matters! For example, decrementing [sellIn] must happen **after** all quality
        // updates are performed.
        val rules = when (item.name) {
            // Aged Brie increases in quality the older it gets.
            // Disambiguate: Increment doubles **after** [sellIn] date.
            "Aged Brie" -> listOf(GildedRule.AgedBrieQualityIncrement, GildedRule.SellInDecrement)
            // Sulfuras does not change, ever.
            "Sulfuras, Hand of Ragnaros" -> emptyList()
            // Backstage passes increase in quality as the concert date approaches.
            "Backstage passes to a TAFKAL80ETC concert" ->
                listOf(GildedRule.BackstageQualityIncrement, GildedRule.SellInDecrement)
            // Kotlin has no real pattern matching... :)
            else -> {
                val multiplier = if (item.name.startsWith("Conjured")) 2 else 1
                listOf(
                    GildedRule.QualityDecrement(multiplier = multiplier),
                    GildedRule.SellInDecrement,
                )
            }
        }

        rules.forEach { it.update(item) }
    }
}
