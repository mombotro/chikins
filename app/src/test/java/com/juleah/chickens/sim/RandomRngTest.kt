package com.juleah.chickens.sim

import org.junit.Assert.assertTrue
import org.junit.Test

class RandomRngTest {
    @Test
    fun `nextLongInRange stays within bounds across many samples`() {
        val rng = RandomRng()
        repeat(1000) {
            val value = rng.nextLongInRange(10L, 20L)
            assertTrue(value in 10L..19L)
        }
    }

    @Test
    fun `nextLongInRange returns min when min equals max`() {
        val rng = RandomRng()
        assertTrue(rng.nextLongInRange(5L, 5L) == 5L)
    }
}
