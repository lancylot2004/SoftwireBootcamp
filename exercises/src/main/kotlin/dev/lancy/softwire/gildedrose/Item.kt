package dev.lancy.softwire.gildedrose

import dev.lancy.softwire.gildedrose.GildedRose.Companion.MAX_QUALITY

data class Item(
    var name: String,
    var sellIn: Int,
    var quality: Int,
)

infix fun Item.shiftQuality(by: Int) {
    this.quality = (this.quality + by).coerceIn(0..MAX_QUALITY)
}
