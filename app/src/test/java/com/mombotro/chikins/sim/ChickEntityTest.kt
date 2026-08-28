package com.mombotro.chikins.sim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChickEntityTest {

    @Test
    fun `growth time is within configured bounds`() {
        val rng = RandomRng()
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 1_000_000L, rng = rng)

        val growthDuration = chick.growUpAtMs - 1_000_000L
        assertTrue(growthDuration >= ChickConfig.MIN_GROWTH_MS)
        assertTrue(growthDuration <= ChickConfig.MAX_GROWTH_MS)
    }

    @Test
    fun `chick has not grown up before its growth time`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        chick.tick(nowMs = chick.growUpAtMs - 1)

        assertFalse(chick.hasGrownUp)
    }

    @Test
    fun `chick grows up once its growth time is reached`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        chick.tick(nowMs = chick.growUpAtMs)

        assertTrue(chick.hasGrownUp)
    }

    @Test
    fun `toChicken carries over id and position with a fresh lifespan`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 42, x = 7.0, y = 8.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        val chicken = chick.toChicken(nowMs = chick.growUpAtMs, rng = rng)

        assertTrue(chicken.id == 42L)
        assertTrue(chicken.x == 7.0)
        assertTrue(chicken.y == 8.0)
        assertTrue(chicken.deathAtMs > chick.growUpAtMs)
    }
}
