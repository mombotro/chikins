package com.mombotro.chikins.sim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChickenEntityTest {

    @Test
    fun `death time is within configured lifespan bounds`() {
        val rng = RandomRng()
        val birth = 1_000_000L
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = birth, rng = rng)

        val lifespan = chicken.deathAtMs - birth
        assertTrue(lifespan >= ChickenConfig.MIN_LIFESPAN_MS)
        assertTrue(lifespan <= ChickenConfig.MAX_LIFESPAN_MS)
    }

    @Test
    fun `chicken is not dead before its death time`() {
        val rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = 0L, rng = rng)

        chicken.tick(nowMs = chicken.deathAtMs - 1)

        assertFalse(chicken.isDead)
    }

    @Test
    fun `chicken dies once its death time is reached`() {
        val rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = 0L, rng = rng)

        chicken.tick(nowMs = chicken.deathAtMs)

        assertTrue(chicken.isDead)
    }
}
