package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `releases the sitting chicken once sitting time elapses, but does not hatch yet`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertNull(egg.sittingChickenIdOrNull())
        assertEquals(EggState.SITTING, egg.state)
    }

    @Test
    fun `moves to hatching only after the move-away delay elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS - 1)
        assertEquals(EggState.SITTING, egg.state)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS)
        assertEquals(EggState.HATCHING, egg.state)
        assertFalse(egg.isReadyToHatch)
    }

    @Test
    fun `isReadyToHatch only becomes true after the hatch duration elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        val hatchStartMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)
        egg.tick(nowMs = hatchStartMs)

        egg.tick(nowMs = hatchStartMs + EggConfig.HATCH_DURATION_MS - 1)
        assertFalse(egg.isReadyToHatch)

        egg.tick(nowMs = hatchStartMs + EggConfig.HATCH_DURATION_MS)
        assertTrue(egg.isReadyToHatch)
    }

    @Test
    fun `hatch produces a chick at the egg position`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 12.0, y = 34.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        val hatchStartMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)
        egg.tick(nowMs = hatchStartMs)
        egg.tick(nowMs = hatchStartMs + EggConfig.HATCH_DURATION_MS)

        val chick = egg.hatch(newChickId = 99, nowMs = hatchStartMs + EggConfig.HATCH_DURATION_MS)

        assertEquals(EggState.HATCHED, egg.state)
        assertEquals(99L, chick.id)
        assertTrue(chick.x == 12.0 && chick.y == 34.0)
    }
}
