package dev.lancy.softwire.gildedrose

sealed class GildedRule {
    abstract fun update(item: Item)

    data class QualityDecrement(
        val multiplier: Int = 1,
        val postSellInMultiplier: Int = 2,
    ) : GildedRule() {
        override fun update(item: Item) {
            val delta = -GildedRose.INCREMENT *
                multiplier *
                if (item.sellIn <= 0) postSellInMultiplier else 1
            item shiftQuality delta
        }
    }

    data object BackstageQualityChanges : GildedRule() {
        override fun update(item: Item) = when (item.sellIn) {
            in Int.MIN_VALUE..0 -> item.quality = 0
            in 1..5 -> item shiftQuality GildedRose.INCREMENT * 3
            in 6..10 -> item shiftQuality GildedRose.INCREMENT * 2
            else -> item shiftQuality GildedRose.INCREMENT
        }
    }

    data object AgedBrieQualityIncrement : GildedRule() {
        override fun update(item: Item) {
            item shiftQuality GildedRose.INCREMENT * if (item.sellIn <= 0) 2 else 1
        }
    }

    data object SellInDecrement : GildedRule() {
        override fun update(item: Item) {
            item.sellIn--
        }
    }
}
