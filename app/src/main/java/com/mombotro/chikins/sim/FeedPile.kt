package com.mombotro.chikins.sim

class FeedPile(
    val id: Long,
    val x: Double,
    val y: Double
) {
    var amount: Int = FeedConfig.INITIAL_AMOUNT
        private set
    val isEmpty: Boolean get() = amount <= 0

    fun consume(amount: Int = FeedConfig.CONSUME_AMOUNT): Boolean {
        this.amount = (this.amount - amount).coerceAtLeast(0)
        return isEmpty
    }
}
