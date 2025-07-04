package dev.lancy.softwire.gildedrose

class GildedRose(
    val items: List<Item>,
) {
    companion object {
        const val INCREMENT = 1
        const val MAX_QUALITY = 50
    }

    fun updateQuality() =
        // Warning, this method is mutating on individual elements of the [items] list.
        items.forEach(::updateItem)

    private fun updateItem(item: Item) {
        // This check is here because we are not allowed to modify [Item].
        require(item.quality >= 0) { "Quality can never be negative!" }

        // Ordering matters! For example, decrementing [sellIn] must happen **after** all quality
        // updates are performed. See spec <pretend there's a link here> for details.
        val rules =
            when (item.name) {
                "Aged Brie" -> listOf(GildedRule.AgedBrieQualityIncrement, GildedRule.SellInDecrement)
                "Sulfuras, Hand of Ragnaros" -> emptyList()
                "Backstage passes to a TAFKAL80ETC concert" ->
                    listOf(GildedRule.BackstageQualityChanges, GildedRule.SellInDecrement)
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
