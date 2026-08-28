package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPileTest {

    @Test
    fun `starts at initial amount`() {
        val pile = FeedPile(id = 1, x = 0.0, y = 0.0)
        assertEquals(FeedConfig.INITIAL_AMOUNT, pile.amount)
        assertFalse(pile.isEmpty)
    }

    @Test
    fun `consume reduces amount and reports empty at zero`() {
        val pile = FeedPile(id = 1, x = 0.0, y = 0.0)
        var empty = false
        repeat(FeedConfig.INITIAL_AMOUNT / FeedConfig.CONSUME_AMOUNT) {
            empty = pile.consume()
        }
        assertTrue(empty)
        assertEquals(0, pile.amount)
    }
}
