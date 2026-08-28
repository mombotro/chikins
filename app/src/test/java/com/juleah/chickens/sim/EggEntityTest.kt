package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EggEntityTest {

    @Test
    fun `starts in waiting state`() {
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = RandomRng())
        assertEquals(EggState.WAITING, egg.state)
    }

    @Test
    fun `startSitting moves egg into sitting state`() {
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = RandomRng())
        egg.startSitting(chickenId = 5, nowMs = 0L)
        assertEquals(EggState.SITTING, egg.state)
        assertEquals(5L, egg.sittingChickenIdOrNull())
    }

    @Test
    fun `stays sitting before its sitting time elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS - 1)

        assertEquals(EggState.SITTING, egg.state)
    }

    @Test
    fun `moves to hatching once its sitting time elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertEquals(EggState.HATCHING, egg.state)
    }

    @Test
    fun `hatch produces a chick at the egg position`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 12.0, y = 34.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        val chick = egg.hatch(newChickId = 99, nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertEquals(EggState.HATCHED, egg.state)
        assertEquals(99L, chick.id)
        assertTrue(chick.x == 12.0 && chick.y == 34.0)
    }
}
